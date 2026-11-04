"""JavaBackendClient 自动化测试。"""

import asyncio

import httpx
import pytest

from app.clients.java_backend import JavaBackendClient, JavaBackendError


def test_get_my_appointments_forwards_jwt_and_query() -> None:
    """验证客户端会正确传递 JWT 和预约查询参数。"""

    async def handler(

        request: httpx.Request,
    ) -> httpx.Response:
        assert request.headers["Authorization"] == "Bearer patient-jwt"
        assert request.url.path == "/api/v1/appointments/my"
        assert dict(request.url.params) == {
            "page": "1",
            "size": "10",
            "status": "已支付",
        }

        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "查询成功",
                "data": {
                    "records": [
                        {
                            "appointmentId": 231,
                            "status": "已支付",
                            "doctorName": "李医生",
                            "department": "内科",
                        }
                    ],
                    "total": 1,
                },
            },
        )

    client = JavaBackendClient(
        base_url="http://java.test/api/v1",
        transport=httpx.MockTransport(handler),
    )

    result = asyncio.run(
        client.get_my_appointments(
            access_token="patient-jwt",
            status="已支付",
        )
    )

    assert result["total"] == 1
    assert result["records"][0]["appointmentId"] == 231



def test_get_my_appointments_reports_expired_login() -> None:
    """验证 Java 返回 401 时，客户端转换为登录失效异常。"""

    async def handler(
        request: httpx.Request,
    ) -> httpx.Response:
        return httpx.Response(
            401,
            json={
                "code": 40100,
                "message": "请先登录",
                "data": None,
            },
        )

    client = JavaBackendClient(
        base_url="http://java.test/api/v1",
        transport=httpx.MockTransport(handler),
    )

    with pytest.raises(JavaBackendError) as error:
        asyncio.run(
            client.get_my_appointments(
                access_token="expired-jwt",
            )
        )

    assert error.value.code == 40100
    assert error.value.message == "登录状态已失效，请重新登录"


def test_search_doctors_forwards_filters() -> None:
    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["Authorization"] == "Bearer patient-jwt"
        assert request.url.path == "/api/v1/doctors"
        assert dict(request.url.params) == {
            "page": "2",
            "size": "5",
            "departmentId": "1",
            "name": "李",
            "date": "2026-09-30",
        }
        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "查询成功",
                "data": {
                    "records": [{"id": 7, "name": "李医生", "remainCount": 3}],
                    "total": 1,
                },
            },
        )

    client = JavaBackendClient(
        "http://java.test/api/v1",
        transport=httpx.MockTransport(handler),
    )
    result = asyncio.run(
        client.search_doctors(
            access_token="patient-jwt",
            department_id=1,
            name="李",
            date="2026-09-30",
            page=2,
            size=5,
        )
    )

    assert result["total"] == 1
    assert result["records"][0]["id"] == 7


def test_create_appointment_posts_source_id() -> None:
    """验证创建预约客户端使用 POST 并传递号源编号。"""

    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.method == "POST"
        assert request.url.path == "/api/v1/appointments"
        assert request.headers["Authorization"] == "Bearer patient-jwt"
        assert request.content == b'{"sourceId":79}'
        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "预约创建成功",
                "data": {
                    "appointmentId": 236,
                    "status": "待支付",
                },
            },
        )

    client = JavaBackendClient(
        "http://java.test/api/v1",
        transport=httpx.MockTransport(handler),
    )

    result = asyncio.run(
        client.create_appointment(
            access_token="patient-jwt",
            source_id=79,
        )
    )

    assert result["appointmentId"] == 236


def test_cancel_appointment_uses_put() -> None:
    """验证取消预约客户端使用 PUT 和正确预约地址。"""

    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.method == "PUT"
        assert request.url.path == "/api/v1/appointments/236/cancel"
        assert request.headers["Authorization"] == "Bearer patient-jwt"
        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "预约已取消",
                "data": {
                    "appointmentId": 236,
                    "status": "取消",
                },
            },
        )

    client = JavaBackendClient(
        "http://java.test/api/v1",
        transport=httpx.MockTransport(handler),
    )

    result = asyncio.run(
        client.cancel_appointment(
            access_token="patient-jwt",
            appointment_id=236,
        )
    )

    assert result["status"] == "取消"


def test_sign_in_posts_appointment_id() -> None:
    """验证签到客户端会向签到接口传递预约编号。"""

    async def handler(request: httpx.Request) -> httpx.Response:
        assert request.method == "POST"
        assert request.url.path == "/api/v1/sign-ins"
        assert request.headers["Authorization"] == "Bearer patient-jwt"
        assert request.content == b'{"appointmentId":236}'
        return httpx.Response(
            200,
            json={
                "code": 200,
                "message": "签到成功",
                "data": {
                    "queueNo": "A006",
                },
            },
        )

    client = JavaBackendClient(
        "http://java.test/api/v1",
        transport=httpx.MockTransport(handler),
    )

    result = asyncio.run(
        client.sign_in(
            access_token="patient-jwt",
            appointment_id=236,
        )
    )

    assert result["queueNo"] == "A006"
