"""使用 Redis 控制同一患者会话的并发请求。"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from secrets import token_hex

from redis.asyncio import Redis

from app.core.exceptions import AppException


class ChatLockManager:
    """管理患者会话级 Redis 分布式锁。"""

    def __init__(self, redis: Redis, *, ttl_seconds: int) -> None:
        self._redis = redis
        self._ttl_seconds = ttl_seconds

    @asynccontextmanager
    async def lock(self, thread_id: str) -> AsyncIterator[None]:
        """同一 thread_id 同一时间只允许一个 Agent 请求。"""

        # 会话并发锁：随机 owner_token 用于标记当前请求对锁的所有权。
        # 释放时必须核对 token，避免旧请求误删后来请求重新获得的锁。
        lock_key = f"chat_lock:{thread_id}"
        owner_token = token_hex(16)
        acquired = await self._redis.set(
            lock_key,
            owner_token,
            nx=True,
            ex=self._ttl_seconds,
        )
        if not acquired:
            raise AppException(
                "当前回答仍在生成，请等待完成后再试",
                code=42901,
                status_code=429,
            )

        try:
            yield
        finally:
            # 会话并发锁：Lua 脚本把“核对所有者”和“删除锁”作为一个原子操作。
            # 不能先 GET 再 DELETE，否则两条命令之间可能发生锁过期和重新加锁。
            await self._redis.eval(
                """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """,
                1,
                lock_key,
                owner_token,
            )


# 会话并发锁：与 Checkpointer 一样，由 FastAPI lifespan 创建和关闭。
_chat_lock_manager: ChatLockManager | None = None


def set_chat_lock_manager(manager: ChatLockManager) -> None:
    """保存应用当前使用的会话并发锁管理器。"""

    global _chat_lock_manager
    _chat_lock_manager = manager


def get_chat_lock_manager() -> ChatLockManager:
    """取得已经初始化完成的会话并发锁管理器。"""

    if _chat_lock_manager is None:
        raise RuntimeError("Redis 会话并发锁尚未初始化")
    return _chat_lock_manager


def clear_chat_lock_manager() -> None:
    """应用关闭时清除会话并发锁管理器。"""

    global _chat_lock_manager
    _chat_lock_manager = None
