"""Redis 聊天历史恢复测试。"""

import asyncio
from types import SimpleNamespace

import pytest
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage

from app.services.chat_service import ChatService


class CurrentPatientClient:
    """返回固定患者身份，避免测试访问真实 Java 后端。"""

    async def get_current_user(self, *, access_token: str) -> dict:
        assert access_token == "patient-jwt"
        return {
            "userId": 1004,
            "role": "patient",
        }


class HistoryAgent:
    """返回包含用户、工具调用和最终回答的模拟 LangGraph 状态。"""

    async def aget_state(self, config: dict) -> SimpleNamespace:
        assert config["configurable"]["thread_id"] == (
            "patient:1004:patient-session"
        )

        return SimpleNamespace(
            values={
                "messages": [
                    HumanMessage(
                        content="这里是较早对话的内部摘要",
                        additional_kwargs={"lc_source": "summarization"},
                    ),
                    HumanMessage(
                        content=(
                            "系统当前日期（北京时间）：2026-10-01\n"
                            "患者消息：帮我查询预约"
                        )
                    ),
                    AIMessage(
                        content="",
                        tool_calls=[
                            {
                                "name": "get_my_appointments",
                                "args": {},
                                "id": "tool-call-1",
                                "type": "tool_call",
                            }
                        ],
                    ),
                    ToolMessage(
                        content='{"success": true}',
                        tool_call_id="tool-call-1",
                    ),
                    AIMessage(content="共查询到 1 条预约。"),
                ]
            },
            tasks=(),
        )


class PendingHistoryAgent:
    """返回一个等待患者确认的预约创建状态。"""

    async def aget_state(self, _config: dict) -> SimpleNamespace:
        return SimpleNamespace(
            values={"messages": []},
            tasks=(
                SimpleNamespace(
                    interrupts=(
                        SimpleNamespace(
                            value={
                                "action_requests": [
                                    {
                                        "name": "create_appointment",
                                        "args": {"source_id": 79},
                                    }
                                ]
                            }
                        ),
                    )
                ),
            ),
        )


def test_get_history_returns_only_visible_messages(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """历史接口只返回患者输入和最终助手回答。"""

    monkeypatch.setattr(
        "app.services.chat_service.get_java_backend_client",
        lambda: CurrentPatientClient(),
    )

    service = ChatService.__new__(ChatService)
    service._agent = HistoryAgent()

    result = asyncio.run(
        service.get_history(
            access_token="patient-jwt",
            session_id="patient-session",
        )
    )

    assert [message.model_dump() for message in result.messages] == [
        {
            "role": "user",
            "content": "帮我查询预约",
        },
        {
            "role": "assistant",
            "content": "共查询到 1 条预约。",
        },
    ]


def test_get_history_restores_pending_confirmation(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """刷新页面后仍能恢复尚未处理的人工确认操作。"""

    monkeypatch.setattr(
        "app.services.chat_service.get_java_backend_client",
        lambda: CurrentPatientClient(),
    )

    service = ChatService.__new__(ChatService)
    service._agent = PendingHistoryAgent()

    result = asyncio.run(
        service.get_history(
            access_token="patient-jwt",
            session_id="patient-session",
        )
    )

    assert result.pending_confirmation is not None
    assert result.pending_confirmation.requires_confirmation is True
    assert result.pending_confirmation.pending_action == {
        "name": "create_appointment",
        "args": {"source_id": 79},
    }
    assert result.pending_confirmation.content == (
        "即将使用号源编号 79 创建预约，请确认是否继续。"
    )
