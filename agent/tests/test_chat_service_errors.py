"""ChatService 模型异常转换测试。"""

import asyncio

import httpx
import pytest
from langchain_openai.chat_models.base import OpenAIPermissionDeniedError
from openai import APIConnectionError, APITimeoutError

from app.core.exceptions import AppException
from app.services.chat_service import ChatService


class FailedAgent:
    """调用时抛出指定模型异常的测试 Agent。"""

    def __init__(self, error: Exception) -> None:
        self.error = error

    async def ainvoke(self, *_args, **_kwargs) -> dict:
        raise self.error


class CurrentPatientClient:
    """为模型异常测试提供固定的已登录患者身份。"""

    async def get_current_user(self, *, access_token: str) -> dict:
        assert access_token == "patient-jwt"
        return {
            "userId": 1000,
            "role": "patient",
        }


@pytest.fixture(autouse=True)
def mock_current_patient(monkeypatch: pytest.MonkeyPatch) -> None:
    """隔离 Java 后端，让当前文件只测试模型异常转换。"""

    monkeypatch.setattr(
        "app.services.chat_service.get_java_backend_client",
        lambda: CurrentPatientClient(),
    )


def create_service(error: Exception) -> ChatService:
    """绕过真实模型创建只用于异常测试的 ChatService。"""

    service = ChatService.__new__(ChatService)
    service._agent = FailedAgent(error)
    return service


def invoke_service(service: ChatService) -> None:
    """使用固定参数执行一次普通患者对话。"""

    asyncio.run(
        service.chat(
            message="你好",
            access_token="patient-jwt",
            session_id="test-session",
            decision=None,
        )
    )


def test_permission_error_becomes_quota_message() -> None:
    """验证额度不足不会直接变成未处理的 500 异常。"""

    response = httpx.Response(
        403,
        request=httpx.Request("POST", "https://model.test/chat"),
    )
    service = create_service(
        OpenAIPermissionDeniedError(
            "insufficient_quota",
            response=response,
            body=None,
        )
    )

    with pytest.raises(AppException) as error:
        invoke_service(service)

    assert error.value.code == 50301
    assert error.value.status_code == 503
    assert error.value.message == "模型服务额度已用完，请联系管理员"


def test_timeout_error_becomes_readable_message() -> None:
    """验证模型超时会转换为可读的统一业务异常。"""

    request = httpx.Request("POST", "https://model.test/chat")
    service = create_service(APITimeoutError(request=request))

    with pytest.raises(AppException) as error:
        invoke_service(service)

    assert error.value.code == 50401
    assert error.value.status_code == 504
    assert error.value.message == "回答生成超时，请稍后重试"


def test_connection_error_becomes_readable_message() -> None:
    """验证模型网络异常会转换为可读的统一业务异常。"""

    request = httpx.Request("POST", "https://model.test/chat")
    service = create_service(
        APIConnectionError(
            request=request,
        )
    )

    with pytest.raises(AppException) as error:
        invoke_service(service)

    assert error.value.code == 50302
    assert error.value.status_code == 503
    assert error.value.message == "暂时无法连接模型服务，请稍后重试"
