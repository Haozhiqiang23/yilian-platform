"""患者支付详情查询工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError


@tool
async def get_payment_by_appointment(
    runtime: ToolRuntime[AgentContext],
    appointment_id: int,
) -> dict[str, Any]:
    """根据预约编号查询当前登录患者自己的支付订单和支付状态。"""
    if appointment_id <= 0:
        return {
            "success": False,
            "message": "预约编号必须是正整数",
        }

    try:
        payment = await get_java_backend_client().get_payment_by_appointment(
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
        "payment": payment,
    }
