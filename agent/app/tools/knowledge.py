"""医院通用流程和注意事项相关的 RAG 知识检索工具。"""

import asyncio
from typing import Any

from langchain_core.tools import tool

from app.clients.dependencies import (
    get_embedding_client,
    get_milvus_knowledge_client,
)
from app.clients.embedding import EmbeddingClientError
from app.clients.milvus import MilvusKnowledgeError
from app.core.config import get_settings
from app.services.knowledge_search import KnowledgeSearchService


@tool
async def search_medical_knowledge(query: str) -> dict[str, Any]:
    """检索医院通用流程、预约规则、取消退款说明和检查注意事项。

    仅用于回答通用知识问题。患者个人预约、支付、排队、处方、检查申请和
    检查报告等实时业务数据，必须使用对应业务查询工具，不能使用本工具。
    """

    if not query.strip():
        return {
            "success": False,
            "message": "知识检索问题不能为空",
        }

    try:
        # 【RAG修复：异常边界】客户端创建也可能因配置缺失或 Milvus 离线失败，
        # 必须和真正的检索调用放在同一个异常处理范围内。
        service = KnowledgeSearchService(
            embedding_client=get_embedding_client(),
            milvus_client=await asyncio.to_thread(
                get_milvus_knowledge_client
            ),
        )
        results = await service.search(query, limit=3)
    except (EmbeddingClientError, MilvusKnowledgeError, ValueError) as exc:
        return {
            "success": False,
            "message": str(exc),
        }

    # 【RAG修复：可配置阈值】真实问题集评估后可以通过 .env 调整，无需改代码。
    minimum_score = get_settings().knowledge_min_relevance_score
    matched_results = [
        result
        for result in results
        if result.score >= minimum_score
    ]

    return {
        "success": True,
        "total": len(matched_results),
        "results": [result.model_dump() for result in matched_results],
    }
