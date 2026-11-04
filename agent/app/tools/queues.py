"""患者排队进度查询工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError


# 预约编号查询当前患者的排队号
@tool
async def get_queue_status(
    runtime: ToolRuntime[AgentContext],
    appointment_id: int,
) -> dict[str, Any]:
    """根据预约编号查询当前患者的排队号、前方人数和当前排队状态。"""
    if appointment_id <= 0:
        return {
            "success": False,
            "message": "预约编号必须是正整数",
        }

    try:
        queue = await get_java_backend_client().get_queue_status(
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
        "queue": queue,
    }
