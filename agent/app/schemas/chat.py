from typing import Any, Literal, Self

from pydantic import BaseModel, Field, model_validator


class ChatRequest(BaseModel):
    """患者发送给智能助手的请求。"""

    message: str | None = Field(
        default=None,
        min_length=1,
        max_length=2000,
        description="患者本次输入",
    )
    session_id: str = Field(
        min_length=1,
        max_length=64,
        description="当前对话的唯一标识",
    )
    decision: Literal["approve", "reject"] | None = Field(
        default=None,
        description="患者对待确认操作的决定",
    )

    @model_validator(mode="after")
    def validate_request_type(self) -> Self:
        """普通消息和确认决定必须二选一。"""

        if self.message is None and self.decision is None:
            raise ValueError("消息和确认决定不能同时为空")

        if self.message is not None and self.decision is not None:
            raise ValueError("消息和确认决定不能同时提交")

        return self


class ChatResponse(BaseModel):
    """智能助手返回给患者的内容。"""

    content: str
    requires_confirmation: bool = False
    pending_action: dict[str, Any] | None = None
    action: dict[str, Any] | None = None


class ChatHistoryMessage(BaseModel):
    """聊天历史中允许返回给前端的一条可见消息。"""

    role: Literal["user", "assistant"]
    content: str


class ChatHistoryResponse(BaseModel):
    """当前患者指定会话的历史消息。"""

    session_id: str
    messages: list[ChatHistoryMessage]
    pending_confirmation: ChatResponse | None = None
