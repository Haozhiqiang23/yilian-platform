from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from langgraph.checkpoint.redis.aio import AsyncRedisSaver
from redis.asyncio import Redis

from app.api.router import api_router
from app.clients.dependencies import close_milvus_knowledge_client
from app.core.chat_lock import (
    ChatLockManager,
    clear_chat_lock_manager,
    set_chat_lock_manager,
)
from app.core.checkpointer import clear_checkpointer, set_checkpointer
from app.core.config import get_settings
from app.core.exceptions import register_exception_handlers
from app.core.observability import ObservabilityMiddleware


# 负责创建和关闭资源
@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    """管理应用启动和关闭期间使用的 Redis Checkpointer。"""
    settings = get_settings()

    # 会话并发锁：独立 Redis 客户端只负责 SET NX 加锁与安全释放，
    # 不与 LangGraph Checkpointer 的内部连接实现耦合。
    lock_redis = Redis.from_url(settings.redis_url, decode_responses=True)
    await lock_redis.ping()
    set_chat_lock_manager(
        ChatLockManager(
            lock_redis,
            ttl_seconds=settings.chat_lock_ttl_seconds,
        )
    )

    try:
        # Redis Checkpointer 必须在异步环境中创建。
        async with AsyncRedisSaver.from_conn_string(
            settings.redis_url,
            # Redis 会话过期：默认七天未使用自动清理；读取时重新计算七天。
            ttl={
                "default_ttl": settings.redis_checkpoint_ttl_minutes,
                "refresh_on_read": True,
            },
        ) as checkpointer:
            # 第一次启动时创建 LangGraph 保存状态所需的索引。
            await checkpointer.asetup()

            # 保存 Checkpointer，供 Agent 工厂读取。
            set_checkpointer(checkpointer)

            # yield 前是启动过程，yield 后是关闭过程。
            yield
    finally:
        # 【RAG修复：连接生命周期】释放网页查询期间复用的 Milvus 连接。
        close_milvus_knowledge_client()
        # 会话并发锁：即使 Checkpointer 初始化失败，也要关闭独立 Redis 连接。
        clear_checkpointer()
        clear_chat_lock_manager()
        await lock_redis.aclose()


app = FastAPI(lifespan=lifespan)

# 可观测性：纯 ASGI 中间件会等 SSE 响应体真正发送结束后再记录总耗时。
app.add_middleware(ObservabilityMiddleware)
register_exception_handlers(app)
app.include_router(api_router, prefix="/api")
