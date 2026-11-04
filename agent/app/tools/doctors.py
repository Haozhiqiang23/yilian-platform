# 查询医生的工具
from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import (
    JavaBackendError,
)


# 查询医生
@tool
async def search_doctors(
    runtime: ToolRuntime[AgentContext],
    department_id: int | None = None,
    doctor_name: str | None = None,
    visit_date: str | None = None,
    page: int = 1,
    size: int = 10,
) -> dict[str, Any]:
    """按科室 ID、医生姓名或就诊日期查询可用医生。
    所有筛选条件都可选；visit_date 使用 YYYY-MM-DD 格式。
    当患者希望找医生、按姓名搜索医生或查询某日有号医生时使用。
    """

    page = max(1, page)
    size = min(max(1, size), 20)

    try:
        data = await get_java_backend_client().search_doctors(
            access_token=runtime.context.access_token,
            department_id=department_id,
            name=doctor_name,
            date=visit_date,
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
        "doctors": records,
    }

# 查询医生详情
@tool
async def  get_doctor_detail(
    runtime: ToolRuntime[AgentContext],
    doctor_id: int,
) -> dict[str, Any]:
    """根据医生 ID 查询医生的科室、职称和个人介绍。"""

    if doctor_id <= 0:
        return {
            "success": False,
            "message": "医生 ID 必须是正整数",
        }

    try:
        doctor = await get_java_backend_client().get_doctor_detail(
            access_token=runtime.context.access_token,
            doctor_id=doctor_id,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }


    return {
        "success": True,
        "doctor": doctor,
    }

# 查询指定号源
@tool
async def search_appointment_sources(
    runtime: ToolRuntime[AgentContext],
    doctor_id: int,
    visit_date: str | None = None,
) -> dict[str, Any]:
    """查询指定医生的预约日期、时段、费用和剩余号源。

    doctor_id 必填；visit_date 可选，格式为 YYYY-MM-DD。
    如果患者只提供医生姓名，应先调用 search_doctors 获取医生 ID。
    """

    if doctor_id <= 0:
        return {
            "success": False,
            "message": "医生 ID 必须是正整数",
        }

    try:
        sources = await get_java_backend_client().search_appointment_sources(
            access_token=runtime.context.access_token,
            doctor_id=doctor_id,
            date=visit_date,
        )
    except JavaBackendError as exc:
        return {
            "success": False,
            "message": exc.message,
            "code": exc.code,
        }

    return {
        "success": True,
        "total": len(sources),
        "sources": sources,
    }






