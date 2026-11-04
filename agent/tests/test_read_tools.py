"""其余患者只读工具的统一回归测试。"""

import asyncio
from types import SimpleNamespace
from typing import Any

import pytest

from app.tools import (
    appointments,
    doctors,
    examinations,
    medical_records,
    messages,
    payments,
    prescriptions,
    queues,
)


class FakeReadClient:
    """记录工具调用参数，并为不同查询方法返回固定数据。"""

    def __init__(self) -> None:
        self.calls: list[tuple[str, dict[str, Any]]] = []

    def __getattr__(self, method_name: str):
        async def call(**kwargs: Any) -> Any:
            self.calls.append((method_name, kwargs))

            if method_name in {"get_my_medical_records", "get_my_messages"}:
                return {"records": [{"id": 1}], "total": 1}
            if method_name == "get_my_examinations":
                return [{"id": 1}]

            return {"id": 1}

        return call


READ_TOOL_CASES = [
    (
        appointments,
        appointments.get_appointment_detail,
        "get_appointment_detail",
        {"appointment_id": 236},
        {"access_token": "patient-jwt", "appointment_id": 236},
    ),
    (
        doctors,
        doctors.get_doctor_detail,
        "get_doctor_detail",
        {"doctor_id": 7},
        {"access_token": "patient-jwt", "doctor_id": 7},
    ),
    (
        queues,
        queues.get_queue_status,
        "get_queue_status",
        {"appointment_id": 236},
        {"access_token": "patient-jwt", "appointment_id": 236},
    ),
    (
        payments,
        payments.get_payment_by_appointment,
        "get_payment_by_appointment",
        {"appointment_id": 236},
        {"access_token": "patient-jwt", "appointment_id": 236},
    ),
    (
        medical_records,
        medical_records.get_my_medical_records,
        "get_my_medical_records",
        {"page": 0, "size": 99},
        {"access_token": "patient-jwt", "page": 1, "size": 20},
    ),
    (
        prescriptions,
        prescriptions.get_prescription_detail,
        "get_prescription_detail",
        {"prescription_id": 9},
        {"access_token": "patient-jwt", "prescription_id": 9},
    ),
    (
        examinations,
        examinations.get_my_examinations,
        "get_my_examinations",
        {},
        {"access_token": "patient-jwt"},
    ),
    (
        examinations,
        examinations.get_examination_report,
        "get_examination_report",
        {"examination_id": 14},
        {"access_token": "patient-jwt", "examination_id": 14},
    ),
    (
        messages,
        messages.get_my_messages,
        "get_my_messages",
        {
            "message_type": "APPOINTMENT",
            "unread_only": True,
            "page": 0,
            "size": 99,
        },
        {
            "access_token": "patient-jwt",
            "message_type": "APPOINTMENT",
            "unread_only": True,
            "page": 1,
            "size": 20,
        },
    ),
    (
        messages,
        messages.get_message_detail,
        "get_message_detail",
        {"message_id": 5},
        {"access_token": "patient-jwt", "message_id": 5},
    ),
]


@pytest.mark.parametrize(
    ("tool_module", "tool", "client_method", "tool_arguments", "expected_arguments"),
    READ_TOOL_CASES,
)
def test_read_tool_forwards_runtime_token_and_arguments(
    monkeypatch: pytest.MonkeyPatch,
    tool_module: Any,
    tool: Any,
    client_method: str,
    tool_arguments: dict[str, Any],
    expected_arguments: dict[str, Any],
) -> None:
    """每个只读工具都必须使用当前运行时 JWT，并规范化业务参数。"""

    client = FakeReadClient()
    monkeypatch.setattr(tool_module, "get_java_backend_client", lambda: client)
    runtime = SimpleNamespace(context=SimpleNamespace(access_token="patient-jwt"))

    result = asyncio.run(tool.coroutine(runtime=runtime, **tool_arguments))

    assert result["success"] is True
    assert client.calls == [(client_method, expected_arguments)]
