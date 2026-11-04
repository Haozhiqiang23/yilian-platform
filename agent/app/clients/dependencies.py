from functools import lru_cache

from app.clients.embedding import EmbeddingClient
from app.clients.java_backend import JavaBackendClient
from app.clients.milvus import MilvusKnowledgeClient
from app.core.config import get_settings


def get_java_backend_client() -> JavaBackendClient:
    settings = get_settings()

    return JavaBackendClient(
        settings.java_backend_url,
        settings.java_backend_timeout_seconds,
    )


@lru_cache
def get_embedding_client() -> EmbeddingClient:
    """创建并复用 Embedding 客户端。"""

    settings = get_settings()
    if not settings.llm_base_url:
        raise ValueError("千问服务地址未配置")

    if not settings.llm_api_key:
        raise ValueError("千问 API Key 未配置")

    return EmbeddingClient(
        model_name=settings.llm_embedding_model,
        base_url=settings.llm_base_url,
        api_key=settings.llm_api_key,
        dimensions=settings.embedding_dimension,
        timeout_seconds=settings.llm_timeout_seconds,
    )


@lru_cache
def get_milvus_knowledge_client() -> MilvusKnowledgeClient:
    """创建并复用医学知识库 Milvus 客户端。"""

    settings = get_settings()
    return MilvusKnowledgeClient(
        uri=settings.milvus_uri,
        token=settings.milvus_token,
        collection_name=settings.milvus_collection_name,
        embedding_dimension=settings.embedding_dimension,
    )


def close_milvus_knowledge_client() -> None:
    """关闭已经创建的 Milvus 客户端，不在关闭阶段创建新连接。"""

    # 【RAG修复：连接生命周期】只有缓存中确实存在客户端时才获取并关闭。
    if get_milvus_knowledge_client.cache_info().currsize == 0:
        return

    get_milvus_knowledge_client().close()
    get_milvus_knowledge_client.cache_clear()
