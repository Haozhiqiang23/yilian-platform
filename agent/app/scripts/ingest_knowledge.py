"""执行本地医学知识文档入库。"""

import asyncio
from pathlib import Path

from app.clients.dependencies import (
    close_milvus_knowledge_client,
    get_embedding_client,
    get_milvus_knowledge_client,
)
from app.services.knowledge_documents import KnowledgeDocumentService
from app.services.knowledge_ingestion import KnowledgeIngestionService


async def main() -> None:
    """组装入库依赖并输出本次执行结果。"""

    project_root = Path(__file__).resolve().parents[2]
    milvus_client = get_milvus_knowledge_client()
    service = KnowledgeIngestionService(
        document_service=KnowledgeDocumentService(
            knowledge_dir=project_root / "knowledge"
        ),
        embedding_client=get_embedding_client(),
        milvus_client=milvus_client,
    )

    try:
        count = await service.ingest()
        total = milvus_client.get_entity_count()
        print(f"本次写入知识分块：{count}")
        print(f"Milvus 当前知识分块总数：{total}")
    finally:
        # 【RAG修复：脚本资源释放】入库失败时也关闭连接并清除缓存。
        close_milvus_knowledge_client()


if __name__ == "__main__":
    asyncio.run(main())
