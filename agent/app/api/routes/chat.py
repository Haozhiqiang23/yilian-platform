import json
from typing import Annotated

from fastapi import APIRouter, Depends, Path
from fastapi.responses import StreamingResponse
from fastapi.security import (
    HTTPAuthorizationCredentials,
    HTTPBearer,
)

from app.core.exceptions import AppException
from app.schemas.chat import ChatHistoryResponse, ChatRequest, ChatResponse
from app.schemas.response import ApiResponse
from app.services.chat_service import ChatService, get_chat_service

router = APIRouter(prefix="/chat", tags=["assistant"])
#拿到jwt auto_error=False没值 返回none
bearer_scheme = HTTPBearer(auto_error=False)
# controller
@router.post("",response_model=ApiResponse[ChatResponse],)
# 有依赖注入和所需参数
async def chat(
    request: ChatRequest,
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer_scheme),
    ],
    #    Depends 依赖注入   可以让下面的service 可以调用chat方法
    service: Annotated[ChatService, Depends(get_chat_service)],
) -> ApiResponse[ChatResponse]:

    if credentials is None:
        raise AppException(
            "请先登录",
            code=40100,
            status_code=401,
        )
    # 调用service的chat方法
    result = await service.chat(
        message=request.message,
        access_token=credentials.credentials,
        session_id=request.session_id,
        decision=request.decision,
    )
    return ApiResponse(data=result)


@router.post("/stream")
async def stream_chat(
    request: ChatRequest,
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer_scheme),
    ],
    service: Annotated[ChatService, Depends(get_chat_service)],
) -> StreamingResponse:
    """使用 SSE 持续返回普通对话的生成内容。"""

    if credentials is None:
        raise AppException(
            "请先登录",
            code=40100,
            status_code=401,
        )
    if request.decision is not None or request.message is None:
        raise AppException(
            "流式接口只接收普通消息",
            code=40002,
            status_code=400,
        )

    async def event_stream():
        # SSE 流式输出：每个事件由 event 和 JSON data 两行组成，
        # 空行表示当前事件结束，前端即可立即解析而不必等待完整回答。
        try:
            async for item in service.stream_chat(
                message=request.message,
                access_token=credentials.credentials,
                session_id=request.session_id,
            ):
                event_name = item["event"]
                event_data = json.dumps(item["data"], ensure_ascii=False)
                yield f"event: {event_name}\ndata: {event_data}\n\n"
        except AppException as exc:
            # SSE 流式输出：身份校验发生在生成器开始迭代后，
            # 此时 HTTP 响应已经建立，需要同样转换成前端可读的 error 事件。
            event_data = json.dumps(
                {
                    "code": exc.code,
                    "message": exc.message,
                },
                ensure_ascii=False,
            )
            yield f"event: error\ndata: {event_data}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )

"""恢复当前登录患者指定会话的可见聊天记录。"""
@router.get(
    "/history/{session_id}",
    response_model=ApiResponse[ChatHistoryResponse],
)
async def get_chat_history(
    session_id: Annotated[
        str,
        Path(min_length=1, max_length=64),
    ],
    credentials: Annotated[
        HTTPAuthorizationCredentials | None,
        Depends(bearer_scheme),
    ],
    service: Annotated[ChatService, Depends(get_chat_service)],
) -> ApiResponse[ChatHistoryResponse]:
    """恢复当前登录患者指定会话的可见聊天记录。"""

    if credentials is None:
        raise AppException(
            "请先登录",
            code=40100,
            status_code=401,
        )


    # 聊天历史恢复：后端会再次验证 JWT，并将会话绑定到真实患者。
    result = await service.get_history(
        access_token=credentials.credentials,
        session_id=session_id,
    )
    return ApiResponse(data=result)
