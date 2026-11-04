"""患者诊疗记录查询工具。"""

from typing import Any

from langchain.tools import ToolRuntime
from langchain_core.tools import tool

from app.agent.context import AgentContext
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError

"""分页查询当前登录患者自己的历史诊疗记录。"""
@tool
async def get_my_medical_records(
    runtime: ToolRuntime[AgentContext],
    page: int = 1,
    size: int = 10,
) -> dict[str, Any]:
    """分页查询当前登录患者自己的历史诊疗记录。"""
    page = max(1, page)
    size = min(max(1, size), 20)

    try:
        data = await get_java_backend_client().get_my_medical_records(
            access_token=runtime.context.access_token,
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
        "medical_records": records,
    }

