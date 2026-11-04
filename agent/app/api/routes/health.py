from fastapi import APIRouter

from app.core.config import get_settings
from app.schemas.response import ApiResponse

router = APIRouter()


@router.get("/health", response_model=ApiResponse[dict[str, str]])
async def health()-> ApiResponse[dict[str, str]] :
    settings = get_settings()
    return ApiResponse(
        data={
            "status": "ok",
            "message": "学习项目运行成功",
            "environment":settings.app_env
        }
    )



