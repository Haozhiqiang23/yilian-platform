"""Redis 会话并发锁测试。"""

import asyncio

import pytest

from app.core.chat_lock import ChatLockManager
from app.core.exceptions import AppException


class FakeRedis:
    """只实现并发锁测试需要的 Redis 命令。"""

    def __init__(self) -> None:
        self.values: dict[str, str] = {}

    async def set(
        self,
        key: str,
        value: str,
        *,
        nx: bool,
        ex: int,
    ) -> bool:
        assert nx is True
        assert ex == 180
        if key in self.values:
            return False
        self.values[key] = value
        return True

    async def eval(
        self,
        _script: str,
        _key_count: int,
        key: str,
        owner_token: str,
    ) -> int:
        if self.values.get(key) != owner_token:
            return 0
        del self.values[key]
        return 1


def test_same_session_cannot_acquire_lock_twice() -> None:
    """相同会话已有请求执行时，第二个请求应被拒绝。"""

    async def run_test() -> None:
        redis = FakeRedis()
        manager = ChatLockManager(redis, ttl_seconds=180)  # type: ignore[arg-type]

        async with manager.lock("patient:1004:session-1"):
            with pytest.raises(AppException) as error:
                async with manager.lock("patient:1004:session-1"):
                    pass

        assert error.value.code == 42901
        assert error.value.status_code == 429

    asyncio.run(run_test())


def test_lock_is_released_after_request_finishes() -> None:
    """请求正常结束后，相同会话应能再次获得锁。"""

    async def run_test() -> None:
        redis = FakeRedis()
        manager = ChatLockManager(redis, ttl_seconds=180)  # type: ignore[arg-type]

        async with manager.lock("patient:1004:session-1"):
            assert redis.values

        assert redis.values == {}
        async with manager.lock("patient:1004:session-1"):
            assert redis.values

    asyncio.run(run_test())


def test_lock_is_released_when_request_fails() -> None:
    """请求抛出异常时 finally 仍应释放锁。"""

    async def run_test() -> None:
        redis = FakeRedis()
        manager = ChatLockManager(redis, ttl_seconds=180)  # type: ignore[arg-type]

        with pytest.raises(RuntimeError):
            async with manager.lock("patient:1004:session-1"):
                raise RuntimeError("模拟 Agent 执行失败")

        assert redis.values == {}

    asyncio.run(run_test())
