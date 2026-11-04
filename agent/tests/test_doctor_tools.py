"""Doctor tool tests without calling the real Java backend."""

import asyncio
from types import SimpleNamespace
from typing import Any

from app.tools import doctors


class FakeDoctorClient:
    """Record tool arguments and return deterministic doctor data."""

    def __init__(self) -> None:
        self.calls: list[tuple[str, dict[str, Any]]] = []

    async def search_doctors(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(("doctors", kwargs))
        return {
            "records": [{"id": 7, "name": "李医生", "department": "内科"}],
            "total": 1,
        }

    async def get_doctor_detail(self, **kwargs: Any) -> dict[str, Any]:
        self.calls.append(("detail", kwargs))
        return {"id": 7, "name": "李医生", "title": "主任医师"}

    async def search_appointment_sources(self, **kwargs: Any) -> list[dict[str, Any]]:
        self.calls.append(("sources", kwargs))
        return [{"sourceId": 28, "remainCount": 6}]


def test_search_doctors_uses_runtime_token_and_normalizes_paging(monkeypatch: Any) -> None:
    client = FakeDoctorClient()
    monkeypatch.setattr(doctors, "get_java_backend_client", lambda: client)
    runtime = SimpleNamespace(context=SimpleNamespace(access_token="patient-jwt"))

    result = asyncio.run(
        doctors.search_doctors.coroutine(
            runtime=runtime,
            department_id=1,
            doctor_name="李",
            visit_date="2026-09-30",
            page=0,
            size=99,
        )
    )

    assert result["success"] is True
    assert result["doctors"][0]["id"] == 7
    assert client.calls[0][1] == {
        "access_token": "patient-jwt",
        "department_id": 1,
        "name": "李",
        "date": "2026-09-30",
        "page": 1,
        "size": 20,
    }


def test_search_sources_uses_runtime_token(monkeypatch: Any) -> None:
    client = FakeDoctorClient()
    monkeypatch.setattr(doctors, "get_java_backend_client", lambda: client)
    runtime = SimpleNamespace(context=SimpleNamespace(access_token="patient-jwt"))

    result = asyncio.run(
        doctors.search_appointment_sources.coroutine(
            runtime=runtime,
            doctor_id=7,
            visit_date="2026-09-30",
        )
    )

    assert result == {
        "success": True,
        "total": 1,
        "sources": [{"sourceId": 28, "remainCount": 6}],
    }
    assert client.calls[0][1]["access_token"] == "patient-jwt"
