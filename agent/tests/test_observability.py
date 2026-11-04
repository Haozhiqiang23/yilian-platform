"""基础可观测性中间件测试。"""

import asyncio
import logging

from app.core.observability import ObservabilityMiddleware, get_request_id


def test_observability_adds_request_id_and_logs_duration(caplog) -> None:
    """响应应携带 request_id，并在响应体结束后记录请求摘要。"""

    request_id_seen_by_app = ""

    async def fake_app(_scope, _receive, send) -> None:
        nonlocal request_id_seen_by_app
        request_id_seen_by_app = get_request_id()
        await send(
            {
                "type": "http.response.start",
                "status": 200,
                "headers": [],
            }
        )
        await send(
            {
                "type": "http.response.body",
                "body": b"ok",
                "more_body": False,
            }
        )

    async def run_test() -> list[dict]:
        sent_messages: list[dict] = []

        async def receive() -> dict:
            return {"type": "http.request", "body": b""}

        async def send(message: dict) -> None:
            sent_messages.append(message)

        middleware = ObservabilityMiddleware(fake_app)
        await middleware(
            {
                "type": "http",
                "method": "POST",
                "path": "/api/chat/stream",
            },
            receive,
            send,
        )
        return sent_messages

    with caplog.at_level(logging.INFO, logger="madp.observability"):
        messages = asyncio.run(run_test())

    response_headers = dict(messages[0]["headers"])
    response_request_id = response_headers[b"x-request-id"].decode("ascii")

    assert len(response_request_id) == 32
    assert request_id_seen_by_app == response_request_id
    assert "path=/api/chat/stream" in caplog.text
    assert "status=200" in caplog.text
    assert "duration_ms=" in caplog.text
    assert "outcome=success" in caplog.text


def test_request_context_is_cleared_after_response() -> None:
    """请求结束后不能把上一个 request_id 泄漏给后续异步任务。"""

    async def fake_app(_scope, _receive, send) -> None:
        await send(
            {
                "type": "http.response.start",
                "status": 204,
                "headers": [],
            }
        )
        await send(
            {
                "type": "http.response.body",
                "body": b"",
                "more_body": False,
            }
        )

    async def run_test() -> None:
        async def receive() -> dict:
            return {"type": "http.request", "body": b""}

        async def send(_message: dict) -> None:
            return None

        middleware = ObservabilityMiddleware(fake_app)
        await middleware(
            {
                "type": "http",
                "method": "GET",
                "path": "/api/health",
            },
            receive,
            send,
        )

    asyncio.run(run_test())
    assert get_request_id() == "-"
