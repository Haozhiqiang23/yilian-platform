"""RAG 文档切分、入库和检索服务测试。"""

import asyncio
from pathlib import Path
from typing import Any

from app.schemas.knowledge import KnowledgeChunk
from app.services.knowledge_documents import KnowledgeDocumentService
from app.services.knowledge_ingestion import KnowledgeIngestionService
from app.services.knowledge_search import KnowledgeSearchService


class FakeEmbeddingClient:
    """返回固定向量，避免测试访问真实 Embedding 服务。"""

    async def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [[float(index), 1.0] for index, _text in enumerate(texts)]

    async def embed_query(self, _query: str) -> list[float]:
        return [0.5, 1.0]


class FakeDocumentService:
    """为入库测试提供一条固定知识分块。"""

    def load_chunks(self) -> list[KnowledgeChunk]:
        return [
            KnowledgeChunk(
                id="chunk-1",
                document_name="常见问题.md",
                category="faq",
                content="如何查看预约？",
                source="https://example.com/help",
                chunk_index=0,
            )
        ]


class FakeMilvusClient:
    """记录服务层传入的数据，不连接真实 Milvus。"""

    def __init__(self) -> None:
        self.collection_checked = False
        self.replaced_rows: list[dict[str, Any]] = []

    def ensure_collection(self) -> bool:
        self.collection_checked = True
        return False

    def replace_documents(self, rows: list[dict[str, Any]]) -> int:
        self.replaced_rows = rows
        return len(rows)

    def search(self, vector: list[float], *, limit: int) -> list[dict[str, Any]]:
        assert vector == [0.5, 1.0]
        assert limit == 2
        return [
            {
                "id": "chunk-1",
                "distance": 0.88,
                "entity": {
                    "document_name": "常见问题.md",
                    "category": "faq",
                    "content": "如何查看预约？",
                    "source": "https://example.com/help",
                    "chunk_index": 0,
                },
            }
        ]


def test_document_service_splits_registered_markdown(tmp_path: Path) -> None:
    document = tmp_path / "常见问题.md"
    document.write_text(
        "# 常见问题\n\n## 预约查询\n\n可以在预约页面查看。\n\n"
        "## 参考资料\n\nhttps://example.com/help\n",
        encoding="utf-8",
    )
    service = KnowledgeDocumentService(knowledge_dir=tmp_path)

    chunks = service.load_chunks()

    assert len(chunks) == 1
    assert chunks[0].category == "faq"
    assert chunks[0].content == "预约查询\n\n可以在预约页面查看。"
    assert chunks[0].source == "https://example.com/help"


def test_ingestion_replaces_old_document_chunks() -> None:
    milvus_client = FakeMilvusClient()
    service = KnowledgeIngestionService(
        document_service=FakeDocumentService(),  # type: ignore[arg-type]
        embedding_client=FakeEmbeddingClient(),  # type: ignore[arg-type]
        milvus_client=milvus_client,  # type: ignore[arg-type]
    )

    count = asyncio.run(service.ingest())

    assert count == 1
    assert milvus_client.collection_checked is True
    assert milvus_client.replaced_rows[0]["id"] == "chunk-1"
    assert milvus_client.replaced_rows[0]["embedding"] == [0.0, 1.0]


def test_search_maps_milvus_hit_to_schema() -> None:
    service = KnowledgeSearchService(
        embedding_client=FakeEmbeddingClient(),  # type: ignore[arg-type]
        milvus_client=FakeMilvusClient(),  # type: ignore[arg-type]
    )

    results = asyncio.run(service.search("怎么查看预约", limit=2))

    assert len(results) == 1
    assert results[0].document_name == "常见问题.md"
    assert results[0].content == "如何查看预约？"
    assert results[0].score == 0.88
