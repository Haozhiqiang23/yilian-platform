"""患者检查申请查询工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError

"""查询当前登录患者自己的检查申请列表和处理状态。"""
@tool
async def get_my_examinations(
    runtime: ToolRuntime[AgentContext],
) -> dict[str, Any]:
    """查询当前登录患者自己的检查申请列表和处理状态。"""
    try:
        examinations = await get_java_backend_client().get_my_examinations(
            access_token=runtime.context.access_token,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    return {
        "success": True,
        "total": len(examinations),
        "examinations": examinations,
    }


@tool
async def get_examination_report(
    runtime: ToolRuntime[AgentContext],
    examination_id: int,
) -> dict[str, Any]:
    """根据检查申请编号查询当前登录患者自己的正式检查报告。"""
    if examination_id <= 0:
        return {
            "success": False,
            "message": "检查申请编号必须是正整数",
        }

    try:
        report = await get_java_backend_client().get_examination_report(
            access_token=runtime.context.access_token,
            examination_id=examination_id,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    return {
        "success": True,
        "report": report,
    }
