"""患者处方详情查询工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError


@tool
async def get_prescription_detail(
    runtime: ToolRuntime[AgentContext],
    prescription_id: int,
) -> dict[str, Any]:
    """根据处方编号查询当前登录患者自己的处方详情。"""
    if prescription_id <= 0:
        return {
            "success": False,
            "message": "处方编号必须是正整数",
        }

    try:
        prescription = await get_java_backend_client().get_prescription_detail(
            access_token=runtime.context.access_token,
            prescription_id=prescription_id,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    return {
        "success": True,
        "prescription": prescription,
    }
