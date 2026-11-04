"""请求编号、耗时日志和跨层日志上下文。"""

import logging
from contextvars import ContextVar
from time import perf_counter
from uuid import uuid4

from starlette.types import ASGIApp, Message, Receive, Scope, Send

logger = logging.getLogger("madp.observability")
# 可观测性：统一开启 madp.* 日志，ChatService 和 Java 客户端无需各自配置。
logging.getLogger("madp").setLevel(logging.INFO)

# 可观测性：ContextVar 在同一个异步请求链路中自动传递 request_id，
# Java 客户端等深层代码无需逐层增加 request_id 参数。
_request_id: ContextVar[str] = ContextVar("request_id", default="-")


def get_request_id() -> str:
    """取得当前请求的链路编号。"""

    return _request_id.get()


class ObservabilityMiddleware:
    """记录 HTTP 请求从进入应用到响应体发送结束的总耗时。"""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        request_id = uuid4().hex
        context_token = _request_id.set(request_id)
        started_at = perf_counter()
        status_code = 500
        response_finished = False

        async def send_with_observability(message: Message) -> None:
            nonlocal status_code, response_finished

            if message["type"] == "http.response.start":
                status_code = message["status"]
                # 可观测性：把 request_id 返回给前端，出现问题时可以用它查日志。
                headers = list(message.get("headers", []))
                headers.append((b"x-request-id", request_id.encode("ascii")))
                message["headers"] = headers

            if (
                message["type"] == "http.response.body"
                and not message.get("more_body", False)
            ):
                response_finished = True
                self._log_request(
                    scope=scope,
                    request_id=request_id,
                    status_code=status_code,
                    started_at=started_at,
                    outcome="success" if status_code < 400 else "failed",
                )

            await send(message)

        try:
            await self.app(scope, receive, send_with_observability)
        except Exception:
            if not response_finished:
                self._log_request(
                    scope=scope,
                    request_id=request_id,
                    status_code=status_code,
                    started_at=started_at,
                    outcome="exception",
                )
            raise
        finally:
            _request_id.reset(context_token)

    @staticmethod
    def _log_request(
        *,
        scope: Scope,
        request_id: str,
        status_code: int,
        started_at: float,
        outcome: str,
    ) -> None:
        """输出不包含患者正文和认证信息的请求摘要。"""

        duration_ms = round((perf_counter() - started_at) * 1000, 2)
        logger.info(
            "http_request request_id=%s method=%s path=%s status=%s "
            "duration_ms=%s outcome=%s",
            request_id,
            scope.get("method", "-"),
            scope.get("path", "-"),
            status_code,
            duration_ms,
            outcome,
        )
