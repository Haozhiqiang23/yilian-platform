"""把本地知识文档向量化后写入 Milvus。"""

from datetime import UTC, datetime
from typing import Any

from app.clients.embedding import EmbeddingClient
from app.clients.milvus import MilvusKnowledgeClient
from app.services.knowledge_documents import KnowledgeDocumentService


class KnowledgeIngestionService:
    """编排文档读取、向量生成和 Milvus 写入。"""

    def __init__(
        self,
        *,
        document_service: KnowledgeDocumentService,
        embedding_client: EmbeddingClient,
        milvus_client: MilvusKnowledgeClient,
    ) -> None:
        self._document_service = document_service
        self._embedding_client = embedding_client
        self._milvus_client = milvus_client

    async def ingest(self) -> int:
        """执行一次完整知识入库，并返回写入的分块数量。"""

        # 第一步：读取 Markdown，并按标题得到 KnowledgeChunk 列表。
        chunks = self._document_service.load_chunks()

        # 第二步：只把正文交给 Embedding 模型，顺序与 chunks 保持一致。
        vectors = await self._embedding_client.embed_documents(
            [chunk.content for chunk in chunks]
        )

        # 第三步：把分块元数据和对应向量组合成 Milvus 的一行数据。
        created_at = datetime.now(UTC).isoformat(timespec="seconds")
        rows: list[dict[str, Any]] = []

        for chunk, vector in zip(chunks, vectors, strict=True):
            rows.append(
                {
                    **chunk.model_dump(),
                    "created_at": created_at,
                    "embedding": vector,
                }
            )

        # 第四步：【RAG修复：文档替换】写入新分块并清理同一文档的失效旧分块。
        self._milvus_client.ensure_collection()
        return self._milvus_client.replace_documents(rows)
