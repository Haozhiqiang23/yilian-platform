"""患者预约相关的 Agent 工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import (
    JavaBackendError,
)


# 预约
@tool
async def get_my_appointments(
    runtime: ToolRuntime[AgentContext],
    status: str | None = None,
    page: int = 1,
    size: int = 10,
)->dict[str,Any]:
    """
    查询当前登录患者自己的预约列表。
    当患者询问“我的预约”“待支付预约”“已完成预约”等问题时使用。
    status 是可选的中文预约状态；用户没有明确状态时不要传。
    page 从 1 开始，size 必须在 1 到 20 之间。
    """

    page = max(1, page)
    size = min(max(1, size), 20)

    try:
        data = await get_java_backend_client().get_my_appointments(
            access_token=runtime.context.access_token,
            status=status,
            page=page,
            size=size,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }
    # 拿出java 数据  把 Java 后端数据整理成更适合 Agent 理解的格式
    records = data.get("records", [])

    return {
        "success": True,
        "total": data.get("total", len(records)),
        "page": page,
        "size": size,
        "appointments": records,
    }


# 预约详情
@tool
async def get_appointment_detail(
    runtime: ToolRuntime[AgentContext],
    appointment_id: int,
) -> dict[str, Any]:
    """根据预约编号查询当前患者自己的预约详情。"""
    if appointment_id <= 0:
        return {
            "success": False,
            "message": "预约编号必须是正整数",
        }
    try:
        appointment = await get_java_backend_client().get_appointment_detail(
            access_token=runtime.context.access_token,
            appointment_id=appointment_id,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    return {
        "success": True,
        "appointment": appointment,
    }







