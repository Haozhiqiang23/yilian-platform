"""预约 Agent 工具自动化测试。"""

import asyncio
from types import SimpleNamespace

from app.agent.context import AgentContext
from app.tools import appointments


class FakeAppointmentClient:
    """代替真实 JavaBackendClient 的测试对象。"""

    def __init__(self) -> None:
        self.received: dict = {}

    async def get_my_appointments(
        self,
        **kwargs,
    ) -> dict:
        self.received = kwargs

        return {
            "records": [
                {
                    "appointmentId": 231,
                    "doctorName": "李医生",
                }
            ],
            "total": 1,
        }


def test_appointment_tool_uses_runtime_token(
    monkeypatch,
) -> None:
    """验证工具从 AgentContext 获取 JWT 并传给客户端。"""

    fake_client = FakeAppointmentClient()

    monkeypatch.setattr(
        appointments,
        "get_java_backend_client",
        lambda: fake_client,
    )

    runtime = SimpleNamespace(
        context=AgentContext(
            access_token="patient-jwt",
        )
    )

    result = asyncio.run(
        appointments.get_my_appointments.coroutine(
            runtime=runtime,
            status=None,
            page=1,
            size=10,
        )
    )

    assert fake_client.received["access_token"] == "patient-jwt"
    assert fake_client.received["status"] is None
    assert result["success"] is True
    assert result["appointments"][0]["appointmentId"] == 231
