"""连接 Milvus 并管理医学知识库 Collection。"""

import json
from typing import Any

from pydantic import SecretStr
from pymilvus import DataType, MilvusClient
from pymilvus.exceptions import MilvusException


class MilvusKnowledgeError(Exception):
    """访问 Milvus 知识库时产生的可预期异常。"""


class MilvusKnowledgeClient:
    """统一管理 Milvus 连接以及医学知识库 Collection。"""

    def __init__(
        self,
        *,
        uri: str,
        collection_name: str,
        embedding_dimension: int,
        token: SecretStr | None = None,
    ) -> None:
        # 连接参数只在客户端创建时读取，后续业务方法不再重复读取配置。
        self._collection_name = collection_name
        self._embedding_dimension = embedding_dimension

        try:
            self._client = MilvusClient(
                uri=uri,
                token=token.get_secret_value() if token else "",
            )
        except (MilvusException, ValueError) as exc:
            raise MilvusKnowledgeError("无法连接知识库，请检查 Milvus 服务") from exc

    def ensure_collection(self) -> bool:
        """确保知识库 Collection 存在，并返回本次是否执行了创建。"""

        try:
            # 已经存在时直接复用，使应用重复启动不会重复建表。
            if self._client.has_collection(self._collection_name):
                # 【RAG修复：结构校验】防止更换向量模型后继续误用旧维度 Collection。
                self._validate_collection()
                return False

            schema = self._create_schema()
            index_params = self._create_index_params()

            self._client.create_collection(
                collection_name=self._collection_name,
                schema=schema,
                index_params=index_params,
            )
        except MilvusException as exc:
            raise MilvusKnowledgeError("创建医学知识库失败，请检查 Milvus 服务") from exc

        return True

    def _validate_collection(self) -> None:
        """校验已存在 Collection 的必需字段、向量维度和索引配置。"""

        description = self._client.describe_collection(self._collection_name)
        fields = {
            field.get("name"): field
            for field in description.get("fields", [])
            if isinstance(field, dict)
        }
        required_fields = {
            "id",
            "document_name",
            "category",
            "content",
            "source",
            "chunk_index",
            "created_at",
            "embedding",
        }
        missing_fields = required_fields - fields.keys()
        if missing_fields:
            names = ", ".join(sorted(missing_fields))
            raise MilvusKnowledgeError(f"医学知识库缺少字段：{names}")

        embedding_field = fields["embedding"]
        dimension = embedding_field.get("params", {}).get("dim")
        if int(dimension or 0) != self._embedding_dimension:
            raise MilvusKnowledgeError(
                "医学知识库向量维度与 Embedding 配置不一致"
            )

        indexes = self._client.list_indexes(
            self._collection_name,
            field_name="embedding",
        )
        if not indexes:
            raise MilvusKnowledgeError("医学知识库缺少向量索引")

        index = self._client.describe_index(
            self._collection_name,
            indexes[0],
        )
        if index.get("metric_type") != "COSINE":
            raise MilvusKnowledgeError("医学知识库相似度算法必须为 COSINE")

    def _create_schema(self):
        """定义文档分块在 Milvus 中保存的字段。"""

        schema = self._client.create_schema(
            auto_id=False,
            enable_dynamic_field=False,
        )

        # id 由入库程序生成，同一文档分块重复入库时可以稳定定位。
        schema.add_field(
            field_name="id",
            datatype=DataType.VARCHAR,
            is_primary=True,
            max_length=64,
        )
        schema.add_field(
            field_name="document_name",
            datatype=DataType.VARCHAR,
            max_length=256,
        )
        schema.add_field(
            field_name="category",
            datatype=DataType.VARCHAR,
            max_length=64,
        )
        schema.add_field(
            field_name="content",
            datatype=DataType.VARCHAR,
            max_length=65535,
        )
        schema.add_field(
            field_name="source",
            datatype=DataType.VARCHAR,
            max_length=2048,
        )
        schema.add_field(
            field_name="chunk_index",
            datatype=DataType.INT64,
        )
        schema.add_field(
            field_name="created_at",
            datatype=DataType.VARCHAR,
            max_length=32,
        )
        schema.add_field(
            field_name="embedding",
            datatype=DataType.FLOAT_VECTOR,
            dim=self._embedding_dimension,
        )

        return schema

    def _create_index_params(self):
        """为向量字段创建余弦相似度索引。"""

        index_params = self._client.prepare_index_params()
        index_params.add_index(
            field_name="embedding",
            index_type="AUTOINDEX",
            metric_type="COSINE",
        )
        return index_params

    def upsert(self, rows: list[dict[str, Any]]) -> int:
        """新增或更新知识分块，并返回本次处理的记录数量。"""

        if not rows:
            raise ValueError("待写入 Milvus 的知识数据不能为空")

        try:
            result = self._client.upsert(
                collection_name=self._collection_name,
                data=rows,
            )
            # flush 确保本批写入完成后能够立即被后续检索读取。
            self._client.flush(collection_name=self._collection_name)
        except MilvusException as exc:
            raise MilvusKnowledgeError("写入医学知识库失败，请稍后重试") from exc

        upsert_count = result.get("upsert_count")
        return upsert_count if isinstance(upsert_count, int) else len(rows)

    def replace_documents(self, rows: list[dict[str, Any]]) -> int:
        """写入最新文档分块，并删除同一文档已经失效的旧分块。"""

        if not rows:
            raise ValueError("待替换的知识数据不能为空")

        document_names = sorted(
            {
                str(row["document_name"])
                for row in rows
                if row.get("document_name")
            }
        )
        if not document_names:
            raise ValueError("知识数据中缺少文档名称")

        # 【RAG修复：旧分块清理】先记录旧 ID；新数据写入成功后再删除失效 ID，
        # 避免正文修改或章节减少后，Milvus 仍检索出过期内容。
        document_filter = (
            "document_name in "
            f"{json.dumps(document_names, ensure_ascii=False)}"
        )

        try:
            existing_rows = self._client.query(
                collection_name=self._collection_name,
                filter=document_filter,
                output_fields=["id"],
            )
        except MilvusException as exc:
            raise MilvusKnowledgeError("查询旧知识分块失败，请稍后重试") from exc

        count = self.upsert(rows)
        old_ids = {
            str(row["id"])
            for row in existing_rows
            if isinstance(row, dict) and row.get("id")
        }
        new_ids = {str(row["id"]) for row in rows if row.get("id")}
        stale_ids = sorted(old_ids - new_ids)

        if stale_ids:
            try:
                self._client.delete(
                    collection_name=self._collection_name,
                    ids=stale_ids,
                )
                self._client.flush(collection_name=self._collection_name)
            except MilvusException as exc:
                raise MilvusKnowledgeError("清理旧知识分块失败，请稍后重试") from exc

        return count

    def get_entity_count(self) -> int:
        """查询当前 Collection 中已经保存的知识分块数量。"""

        try:
            stats = self._client.get_collection_stats(self._collection_name)
        except MilvusException as exc:
            raise MilvusKnowledgeError("查询医学知识库统计信息失败") from exc

        row_count = stats.get("row_count", 0)
        return int(row_count)

    def search(
        self,
        query_vector: list[float],
        *,
        limit: int = 3,
    ) -> list[dict[str, Any]]:
        """根据查询向量返回最相似的知识分块。"""

        if len(query_vector) != self._embedding_dimension:
            raise ValueError("查询向量维度与 Milvus Collection 不一致")

        if limit <= 0:
            raise ValueError("检索数量 limit 必须大于 0")

        try:
            results = self._client.search(
                collection_name=self._collection_name,
                data=[query_vector],
                anns_field="embedding",
                limit=limit,
                search_params={"metric_type": "COSINE", "params": {}},
                output_fields=[
                    "document_name",
                    "category",
                    "content",
                    "source",
                    "chunk_index",
                ],
            )
        except MilvusException as exc:
            raise MilvusKnowledgeError("检索医学知识库失败，请稍后重试") from exc

        # 本次只提交一个查询向量，因此取二维结果中的第一组命中记录。
        return results[0] if results else []

    def close(self) -> None:
        """关闭 Milvus 客户端持有的连接。"""

        self._client.close()
