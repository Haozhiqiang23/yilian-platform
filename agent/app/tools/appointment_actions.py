"""患者预约写操作相关的 Agent 工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError


# 创建正式预约
@tool(response_format="content_and_artifact")
async def create_appointment(
    runtime: ToolRuntime[AgentContext],
    source_id: int,
) -> tuple[str, dict[str, Any]]:
    """根据患者已经确认的号源编号创建正式预约。

    这是会修改真实业务数据的写操作。
    只有患者已经明确确认预约时才能执行。
    source_id 必须来自真实号源查询结果，不能自行编造。
    """

    # 模型生成的工具参数不能直接信任，先进行基础校验。
    if source_id <= 0:
        message = "号源编号必须是正整数"
        return message, {
            "success": False,
            "message": message,
        }

    try:
        appointment = await get_java_backend_client().create_appointment(
            access_token=runtime.context.access_token,
            source_id=source_id,
        )
    except JavaBackendError as exc:
        return exc.message, {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    appointment_id = appointment.get("appointmentId")
    status = appointment.get("status")
    content = f"预约创建成功，预约编号：{appointment_id}，状态：{status}。"

    return content, {
        "success": True,
        "message": "预约创建成功",
        "appointment": appointment,
    }


# 取消预约
@tool(response_format="content_and_artifact")
async def cancel_appointment(
    runtime: ToolRuntime[AgentContext],
    appointment_id: int,
) -> tuple[str, dict[str, Any]]:
    """根据患者已经确认的预约编号取消预约。

    这是会修改真实业务数据的写操作。
    只有患者已经明确要求取消，并完成最终确认时才能执行。
    appointment_id 必须来自当前患者的真实预约数据，不能自行编造。
    """

    # 模型生成的工具参数不能直接信任，先进行基础校验。
    if appointment_id <= 0:
        message = "预约编号必须是正整数"
        return message, {
            "success": False,
            "message": message,
        }

    try:
        appointment = await get_java_backend_client().cancel_appointment(
            access_token=runtime.context.access_token,
            appointment_id=appointment_id,
        )
    except JavaBackendError as exc:
        return exc.message, {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    status = appointment.get("status")
    content = f"预约取消成功，预约编号：{appointment_id}，状态：{status}。"

    return content, {
        "success": True,
        "message": "预约取消成功",
        "appointment": appointment,
    }


# 预约签到
@tool(response_format="content_and_artifact")
async def sign_in(
    runtime: ToolRuntime[AgentContext],
    appointment_id: int,
) -> tuple[str, dict[str, Any]]:
    """根据患者已经确认的预约编号完成签到并进入排队。

    这是会修改真实业务数据的写操作。
    只有患者已经明确要求签到，并完成最终确认时才能执行。
    appointment_id 必须来自当前患者的真实预约数据，不能自行编造。
    """

    # 模型生成的工具参数不能直接信任，先进行基础校验。
    if appointment_id <= 0:
        message = "预约编号必须是正整数"
        return message, {
            "success": False,
            "message": message,
        }

    try:
        queue = await get_java_backend_client().sign_in(
            access_token=runtime.context.access_token,
            appointment_id=appointment_id,
        )
    except JavaBackendError as exc:
        return exc.message, {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    queue_no = queue.get("queueNo")
    content = f"签到成功，预约编号：{appointment_id}，排队号：{queue_no}。"

    return content, {
        "success": True,
        "message": "签到成功",
        "appointment_id": appointment_id,
        "queue": queue,
    }
