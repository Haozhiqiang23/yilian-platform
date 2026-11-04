"""根据患者问题检索相关医学知识片段。"""

import asyncio

from app.clients.embedding import EmbeddingClient
from app.clients.milvus import MilvusKnowledgeClient
from app.schemas.knowledge import KnowledgeSearchResult


class KnowledgeSearchService:
    """编排查询向量生成与 Milvus 相似度检索。"""

    def __init__(
        self,
        *,
        embedding_client: EmbeddingClient,
        milvus_client: MilvusKnowledgeClient,
    ) -> None:
        self._embedding_client = embedding_client
        self._milvus_client = milvus_client

    async def search(
        self,
        query: str,
        *,
        limit: int = 3,
    ) -> list[KnowledgeSearchResult]:
        """把自然语言问题转换成向量，并返回最相似的知识分块。"""

        # 第一步：患者问题和文档入库时使用同一个 Embedding 模型。
        query_vector = await self._embedding_client.embed_query(query)

        # 第二步：使用余弦相似度查找最接近的知识分块。
        # 【RAG修复：异步阻塞】pymilvus 当前调用是同步 I/O，放入工作线程，
        # 避免一次向量检索阻塞 FastAPI 事件循环中的其他患者请求。
        hits = await asyncio.to_thread(
            self._milvus_client.search,
            query_vector,
            limit=limit,
        )
        results: list[KnowledgeSearchResult] = []

        for hit in hits:
            entity = hit.get("entity")
            if not isinstance(entity, dict):
                continue

            results.append(
                KnowledgeSearchResult(
                    id=str(hit.get("id", "")),
                    document_name=str(entity.get("document_name", "")),
                    category=str(entity.get("category", "")),
                    content=str(entity.get("content", "")),
                    source=str(entity.get("source", "")),
                    chunk_index=int(entity.get("chunk_index", 0)),
                    score=float(hit.get("distance", 0.0)),
                )
            )

        return results
