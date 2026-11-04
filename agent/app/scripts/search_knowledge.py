"""从命令行验证医学知识库相似度检索。"""

import asyncio
import sys

from app.clients.dependencies import (
    close_milvus_knowledge_client,
    get_embedding_client,
    get_milvus_knowledge_client,
)
from app.services.knowledge_search import KnowledgeSearchService


async def main() -> None:
    """检索命令行传入的问题并打印最相关知识片段。"""

    query = " ".join(sys.argv[1:]).strip() or "怎么取消预约"
    service = KnowledgeSearchService(
        embedding_client=get_embedding_client(),
        milvus_client=get_milvus_knowledge_client(),
    )
    try:
        results = await service.search(query, limit=3)

        print(f"查询问题：{query}")
        print(f"命中数量：{len(results)}")

        for index, result in enumerate(results, start=1):
            print(f"\n{index}. {result.document_name}，相似度：{result.score:.4f}")
            print(result.content)
    finally:
        # 【RAG修复：脚本资源释放】检索异常时同样关闭连接并清除缓存。
        close_milvus_knowledge_client()


if __name__ == "__main__":
    asyncio.run(main())
