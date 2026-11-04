import logging

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse

from app.core.observability import get_request_id
from app.schemas.response import ApiResponse

logger = logging.getLogger("madp.exceptions")


class AppException(Exception):
    """项目中可以预期的业务异常。"""
    def __init__(
            self,
            message: str,
            *,
            code:int = 40000,
            status_code:int = 400,
    )->None:
        super().__init__(message)
        self.message = message
        self.code = code
        self.status_code = status_code


def register_exception_handlers(app: FastAPI)->None:
    """给 FastAPI 应用注册统一异常处理器。"""
    @app.exception_handler(AppException)
    async def handler_app_exception(
            _: Request,
            exc: AppException,
    )-> JSONResponse:

        # 【RAG修复：统一异常】参数统一命名为 exc，避免异常处理器自身再次抛出 NameError。
        # 可观测性：只记录业务错误码和 request_id，不记录患者问题正文。
        logger.warning(
            "app_exception request_id=%s code=%s status=%s",
            get_request_id(),
            exc.code,
            exc.status_code,
        )

        response = ApiResponse[object](
            code=exc.code,
            message=exc.message,
            data=None,
        )

        return JSONResponse(
            status_code=exc.status_code,
            #response.model_dump() 把 Pydantic 对象转换成 Python 字典。
            content=jsonable_encoder(response.model_dump()),
        )
