"""患者站内消息列表查询工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError


@tool
async def get_my_messages(
    runtime: ToolRuntime[AgentContext],
    message_type: str | None = None,
    unread_only: bool = False,
    page: int = 1,
    size: int = 10,
) -> dict[str, Any]:
    """分页查询当前登录患者的消息列表，可按类型或仅未读筛选。"""
    page = max(1, page)
    size = min(max(1, size), 20)

    try:
        data = await get_java_backend_client().get_my_messages(
            access_token=runtime.context.access_token,
            message_type=message_type,
            unread_only=unread_only,
            page=page,
            size=size,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    records = data.get("records", [])

    return {
        "success": True,
        "total": data.get("total", len(records)),
        "page": page,
        "size": size,
        "messages": records,
    }


@tool
async def get_message_detail(
    runtime: ToolRuntime[AgentContext],
    message_id: int,
) -> dict[str, Any]:
    """根据消息编号查询当前登录患者自己的消息详情。"""
    if message_id <= 0:
        return {
            "success": False,
            "message": "消息编号必须是正整数",
        }

    try:
        message = await get_java_backend_client().get_message_detail(
            access_token=runtime.context.access_token,
            message_id=message_id,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    return {
        "success": True,
        "message_detail": message,
    }
