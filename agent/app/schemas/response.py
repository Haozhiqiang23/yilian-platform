from pydantic import BaseModel


class ApiResponse[DataT](BaseModel):
    code: int = 200
    message: str = "操作成功"
    data : DataT | None = None
