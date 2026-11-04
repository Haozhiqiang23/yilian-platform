"""预约写操作 Agent 工具自动化测试。"""

import asyncio
from types import SimpleNamespace
from typing import Any

from app.agent.context import AgentContext
from app.clients.java_backend import JavaBackendError
from app.tools import appointment_actions


class FakeAppointmentActionClient:
    """代替真实 Java 后端，记录工具传入的参数。"""

    def __init__(self) -> None:
        self.calls: list[tuple[str, dict[str, Any]]] = []

    async def create_appointment(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(("create", kwargs))
        return {
            "appointmentId": 236,
            "status": "待支付",
        }

    async def cancel_appointment(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(("cancel", kwargs))
        return {
            "appointmentId": 236,
            "status": "取消",
        }

    async def sign_in(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(("sign_in", kwargs))
        return {
            "queueNo": "A006",
        }


def create_runtime() -> SimpleNamespace:
    """创建带患者 JWT 的测试运行上下文。"""

    return SimpleNamespace(
        context=AgentContext(
            access_token="patient-jwt",
        )
    )


def test_create_appointment_forwards_token_and_source_id(monkeypatch: Any) -> None:
    """验证创建预约工具会透传 JWT 和号源编号。"""

    client = FakeAppointmentActionClient()
    monkeypatch.setattr(
        appointment_actions,
        "get_java_backend_client",
        lambda: client,
    )

    content, artifact = asyncio.run(
        appointment_actions.create_appointment.coroutine(
            runtime=create_runtime(),
            source_id=79,
        )
    )

    assert client.calls == [
        (
            "create",
            {
                "access_token": "patient-jwt",
                "source_id": 79,
            },
        )
    ]
    assert "236" in content
    assert artifact["success"] is True
    assert artifact["appointment"]["status"] == "待支付"


def test_cancel_appointment_forwards_token_and_appointment_id(
    monkeypatch: Any,
) -> None:
    """验证取消预约工具只取消当前患者指定的预约。"""

    client = FakeAppointmentActionClient()
    monkeypatch.setattr(
        appointment_actions,
        "get_java_backend_client",
        lambda: client,
    )

    content, artifact = asyncio.run(
        appointment_actions.cancel_appointment.coroutine(
            runtime=create_runtime(),
            appointment_id=236,
        )
    )

    assert client.calls == [
        (
            "cancel",
            {
                "access_token": "patient-jwt",
                "appointment_id": 236,
            },
        )
    ]
    assert "取消成功" in content
    assert artifact["success"] is True


def test_sign_in_forwards_token_and_appointment_id(monkeypatch: Any) -> None:
    """验证签到工具会返回 Java 后端生成的真实排队号。"""

    client = FakeAppointmentActionClient()
    monkeypatch.setattr(
        appointment_actions,
        "get_java_backend_client",
        lambda: client,
    )

    content, artifact = asyncio.run(
        appointment_actions.sign_in.coroutine(
            runtime=create_runtime(),
            appointment_id=236,
        )
    )

    assert client.calls == [
        (
            "sign_in",
            {
                "access_token": "patient-jwt",
                "appointment_id": 236,
            },
        )
    ]
    assert "A006" in content
    assert artifact["success"] is True
    assert artifact["appointment_id"] == 236


def test_write_tools_reject_invalid_ids(monkeypatch: Any) -> None:
    """验证模型传入非法编号时，不会调用真实 Java 后端。"""

    client = FakeAppointmentActionClient()
    monkeypatch.setattr(
        appointment_actions,
        "get_java_backend_client",
        lambda: client,
    )

    _, create_result = asyncio.run(
        appointment_actions.create_appointment.coroutine(
            runtime=create_runtime(),
            source_id=0,
        )
    )
    _, cancel_result = asyncio.run(
        appointment_actions.cancel_appointment.coroutine(
            runtime=create_runtime(),
            appointment_id=-1,
        )
    )
    _, sign_result = asyncio.run(
        appointment_actions.sign_in.coroutine(
            runtime=create_runtime(),
            appointment_id=0,
        )
    )

    assert client.calls == []
    assert create_result["success"] is False
    assert cancel_result["success"] is False
    assert sign_result["success"] is False


def test_sign_in_returns_java_business_error(monkeypatch: Any) -> None:
    """验证 Java 拒绝签到时，工具返回具体原因而不是抛出系统异常。"""

    class FailedSignInClient:
        async def sign_in(self, **_: Any) -> dict[str, Any]:
            raise JavaBackendError(
                "当前不在签到时间范围",
                code=409,
            )

    monkeypatch.setattr(
        appointment_actions,
        "get_java_backend_client",
        lambda: FailedSignInClient(),
    )

    content, artifact = asyncio.run(
        appointment_actions.sign_in.coroutine(
            runtime=create_runtime(),
            appointment_id=236,
        )
    )

    assert content == "当前不在签到时间范围"
    assert artifact == {
        "success": False,
        "message": "当前不在签到时间范围",
        "code": 409,
    }
