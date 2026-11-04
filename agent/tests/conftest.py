"""测试套件共用依赖。"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import pytest


class NoOpChatLockManager:
    """测试默认使用的不访问真实 Redis 的会话锁。"""

    @asynccontextmanager
    async def lock(self, _thread_id: str) -> AsyncIterator[None]:
        yield


@pytest.fixture(autouse=True)
def mock_chat_lock_manager(monkeypatch: pytest.MonkeyPatch) -> None:
    """业务测试默认绕过 Redis，只测试各自负责的逻辑。"""

    monkeypatch.setattr(
        "app.services.chat_service.get_chat_lock_manager",
        lambda: NoOpChatLockManager(),
    )
