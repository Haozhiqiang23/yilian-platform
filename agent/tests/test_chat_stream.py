"""SSE 流式对话服务测试。"""

import asyncio
from types import SimpleNamespace

import pytest
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage

from app.services.chat_service import ChatService


class CurrentPatientClient:
    """为流式测试提供固定的当前患者。"""

    async def get_current_user(self, *, access_token: str) -> dict:
        assert access_token == "patient-jwt"
        return {
            "userId": 1004,
            "role": "patient",
        }


class StreamingAgent:
    """模拟主模型逐段输出，并在结束后提供最终状态。"""

    async def astream(self, _input: dict, *, config: dict, **_kwargs):
        assert config["configurable"]["thread_id"] == (
            "patient:1004:patient-session"
        )

        yield AIMessageChunk(content="你"), {}
        # 上下文摘要模型的内部输出不能显示给患者。
        yield AIMessageChunk(content="内部摘要"), {"lc_source": "summarization"}
        yield AIMessageChunk(content="好"), {}

    async def aget_state(self, _config: dict) -> SimpleNamespace:
        return SimpleNamespace(
            values={
                "messages": [
                    HumanMessage(content="患者消息：你好"),
                    AIMessage(content="你好"),
                ]
            },
            tasks=(),
        )


def test_stream_chat_emits_tokens_and_final_response(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """主模型片段应逐个返回，结束时返回统一 ChatResponse。"""

    monkeypatch.setattr(
        "app.services.chat_service.get_java_backend_client",
        lambda: CurrentPatientClient(),
    )
    service = ChatService.__new__(ChatService)
    service._agent = StreamingAgent()

    async def collect_events() -> list[dict]:
        return [
            event
            async for event in service.stream_chat(
                message="你好",
                access_token="patient-jwt",
                session_id="patient-session",
            )
        ]

    events = asyncio.run(collect_events())

    assert events == [
        {
            "event": "start",
            "data": {"session_id": "patient-session"},
        },
        {
            "event": "token",
            "data": {"content": "你"},
        },
        {
            "event": "token",
            "data": {"content": "好"},
        },
        {
            "event": "done",
            "data": {
                "content": "你好",
                "requires_confirmation": False,
                "pending_action": None,
                "action": None,
            },
        },
    ]
