import os
from functools import lru_cache

from dotenv import load_dotenv
from pydantic import BaseModel, SecretStr

load_dotenv()


class Settings(BaseModel):
    app_name: str = "MADP Assistant Learning"
    app_env: str = "development"

    java_backend_url: str = "http://127.0.0.1:8080/api/v1"
    # 给 Java 后端请求增加超时配置
    java_backend_timeout_seconds: float = 10.0
    # Redis 用于保存 Agent 会话和执行状态
    redis_url: str = "redis://127.0.0.1:6379/0"
    # Redis 会话过期：Checkpoint 七天没有被读取后自动清理。
    redis_checkpoint_ttl_minutes: int = 7 * 24 * 60
    # 会话并发锁：异常退出后最多保留指定秒数，防止形成永久死锁。
    chat_lock_ttl_seconds: int = 180

    # RAG 知识库：Milvus 独立保存文档分块和对应向量。
    milvus_uri: str = "http://127.0.0.1:19530"
    # 本地 Milvus 未开启鉴权时可以不配置 Token。
    milvus_token: SecretStr | None = None
    # Collection 名称集中配置，业务代码中不重复写字符串。
    milvus_collection_name: str = "medical_knowledge"
    # 该维度必须与后续选用的 Embedding 模型输出维度一致。
    embedding_dimension: int = 1024
    # RAG 检索结果低于该余弦相似度时，不交给 Agent 生成回答。
    knowledge_min_relevance_score: float = 0.45

    # 上下文控制（第一层）：达到该 Token 数量后开始清理较早的工具结果。
    context_editing_trigger_tokens: int = 5000
    # 上下文控制（第一层）：始终保留最近几次完整工具调用，避免影响当前任务。
    context_editing_keep_tool_uses: int = 3
    # 上下文控制（第一层）：每次至少清理这么多 Token，避免频繁进行小范围清理。
    context_editing_clear_at_least_tokens: int = 1000

    llm_model: str = "qwen3.5-plus"
    # 上下文控制（第二层）：长对话摘要使用独立模型，避免与主对话模型混淆。
    llm_summary_model: str = "qwen3.8-27b"
    # RAG 向量化：把文档片段和患者问题转换成同维度向量。
    llm_embedding_model: str = "qwen3.7-text-embedding-flash"
    llm_base_url: str = ""
    llm_api_key: SecretStr | None = None
    llm_timeout_seconds: float = 30.0

    # 上下文控制（第二层）：超过阈值后总结较早对话，并保留最近消息原文。
    summarization_trigger_tokens: int = 8000
    summarization_keep_messages: int = 12



@lru_cache
def get_settings() -> Settings:
    return Settings(
        app_name=os.getenv("APP_NAME", "MADP Assistant Learning"),
        app_env=os.getenv("APP_ENV", "development"),
        java_backend_url=os.getenv("JAVA_BACKEND_URL","http://127.0.0.1:8080/api/v1",),
        java_backend_timeout_seconds=float(os.getenv("JAVA_BACKEND_TIMEOUT_SECONDS", "10")),
        redis_url=os.getenv("REDIS_URL", "redis://127.0.0.1:6379/0"),
        redis_checkpoint_ttl_minutes=int(
            os.getenv("REDIS_CHECKPOINT_TTL_MINUTES", str(7 * 24 * 60))
        ),
        chat_lock_ttl_seconds=int(os.getenv("CHAT_LOCK_TTL_SECONDS", "180")),
        milvus_uri=os.getenv("MILVUS_URI", "http://127.0.0.1:19530"),
        milvus_token=(
            SecretStr(value)
            if (value := os.getenv("MILVUS_TOKEN"))
            else None
        ),
        milvus_collection_name=os.getenv(
            "MILVUS_COLLECTION_NAME",
            "medical_knowledge",
        ),
        embedding_dimension=int(os.getenv("EMBEDDING_DIMENSION", "1024")),
        knowledge_min_relevance_score=float(
            os.getenv("KNOWLEDGE_MIN_RELEVANCE_SCORE", "0.45")
        ),
        context_editing_trigger_tokens=int(
            os.getenv("CONTEXT_EDITING_TRIGGER_TOKENS", "5000")
        ),
        context_editing_keep_tool_uses=int(
            os.getenv("CONTEXT_EDITING_KEEP_TOOL_USES", "3")
        ),
        context_editing_clear_at_least_tokens=int(
            os.getenv("CONTEXT_EDITING_CLEAR_AT_LEAST_TOKENS", "1000")
        ),
        llm_model=os.getenv("LLM_MODEL", "qwen3.8-flash"),
        llm_summary_model=os.getenv("LLM_MODEL2", "qwen3.8-27b"),
        llm_embedding_model=os.getenv(
            "LLM_MODEL3",
            "qwen3.7-text-embedding-flash",
        ),
        llm_base_url=os.getenv("LLM_BASE_URL", ""),
        llm_api_key=os.getenv("LLM_API_KEY") or None,
        llm_timeout_seconds=float(os.getenv("LLM_TIMEOUT_SECONDS", "30")),
        summarization_trigger_tokens=int(
            os.getenv("SUMMARIZATION_TRIGGER_TOKENS", "8000")
        ),
        summarization_keep_messages=int(
            os.getenv("SUMMARIZATION_KEEP_MESSAGES", "12")
        ),
    )
