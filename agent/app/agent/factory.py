from functools import lru_cache
from typing import Any

from langchain.agents import create_agent
from langchain.agents.middleware import (
    ClearToolUsesEdit,
    ContextEditingMiddleware,
    HumanInTheLoopMiddleware,
    SummarizationMiddleware,
    ToolCallLimitMiddleware,
)
from langchain_openai import ChatOpenAI

from app.agent.context import AgentContext
from app.core.checkpointer import get_checkpointer
from app.core.config import get_settings
from app.core.exceptions import AppException
from app.prompts.conversation_summary import CONVERSATION_SUMMARY_PROMPT
from app.prompts.patient_assistant import PATIENT_ASSISTANT_SYSTEM_PROMPT
from app.tools.appointment_actions import (
    cancel_appointment,
    create_appointment,
    sign_in,
)
from app.tools.appointments import get_appointment_detail, get_my_appointments
from app.tools.doctors import (
    get_doctor_detail,
    search_appointment_sources,
    search_doctors,
)
from app.tools.examinations import get_examination_report, get_my_examinations
from app.tools.knowledge import search_medical_knowledge
from app.tools.medical_records import get_my_medical_records
from app.tools.messages import get_message_detail, get_my_messages
from app.tools.payments import get_payment_by_appointment
from app.tools.prescriptions import get_prescription_detail
from app.tools.queues import get_queue_status


def create_qwen_model(
    *,
    model_name: str | None = None,
    temperature: float = 0.2,
) -> ChatOpenAI:
    """创建主对话或内部摘要使用的千问模型客户端。"""

    settings = get_settings()
    if not settings.llm_base_url:
        raise AppException(
            "千问服务地址未配置",
            code=50010,
            status_code=500,
        )
    if not settings.llm_api_key:
        raise AppException(
            "千问 API Key 未配置",
            code=50011,
            status_code=500,
        )
    return ChatOpenAI(
        model=model_name or settings.llm_model,
        api_key=settings.llm_api_key.get_secret_value(),
        base_url=settings.llm_base_url.rstrip("/"),
        timeout=settings.llm_timeout_seconds,
        max_retries=1,
        temperature=temperature,
    )

@lru_cache
def get_patient_agent() -> Any:
    settings = get_settings()

    # 主对话模型负责理解患者问题、决定是否调用工具以及生成最终回答。
    chat_model = create_qwen_model()
    # 上下文控制（第二层）：摘要模型只在长对话达到阈值时调用，
    # 不参与普通短对话，也不会直接向患者输出内容。
    summary_model = create_qwen_model(
        model_name=settings.llm_summary_model,
        temperature=0,
    )

    tools: list[Any] = [
        get_my_appointments,
        search_doctors,
        get_doctor_detail,
        search_appointment_sources,
        get_appointment_detail,
        get_queue_status,
        get_my_medical_records,
        get_my_examinations,
        get_examination_report,
        get_prescription_detail,
        get_my_messages,
        get_message_detail,
        get_payment_by_appointment,
        create_appointment,
        cancel_appointment,
        sign_in,
        search_medical_knowledge,
    ]

    return create_agent(
        model=chat_model,
        tools=tools,
        system_prompt=PATIENT_ASSISTANT_SYSTEM_PROMPT,
        context_schema=AgentContext,
        middleware=[
            # 上下文控制（第一层）：只清理较早的工具调用结果，不删除患者问题、
            # 助手最终回答、Redis Checkpoint 或正在等待人工确认的动作。
            # 这一层不调用额外模型，主要用于减少医生、预约、报告等工具返回的
            # 大段 JSON 长期占用千问上下文。
            ContextEditingMiddleware(
                edits=[
                    ClearToolUsesEdit(
                        # 上下文估算达到阈值后才开始清理，短对话不会受到影响。
                        trigger=settings.context_editing_trigger_tokens,
                        # 最近几次工具调用通常与当前追问有关，因此保留完整内容。
                        keep=settings.context_editing_keep_tool_uses,
                        # 一次至少清理指定 Token，避免每轮只清理少量内容。
                        clear_at_least=(
                            settings.context_editing_clear_at_least_tokens
                        ),
                        # 保留工具输入参数，便于模型理解之前查询了什么；
                        # 只清理体积通常更大的旧工具输出。
                        clear_tool_inputs=False,
                        # 被清理的工具输出使用明确占位符，避免模型误认为数据缺失。
                        placeholder="[较早的工具结果已清理]",
                    )
                ],
                # 使用近似 Token 统计，不额外请求模型，性能开销较小。
                token_count_method="approximate",
            ),
            # 上下文控制（第二层）：第一层清理旧工具结果后，如果上下文仍然
            # 达到摘要阈值，则使用独立摘要模型压缩较早对话。
            SummarizationMiddleware(
                model=summary_model,
                trigger=(
                    "tokens",
                    settings.summarization_trigger_tokens,
                ),
                # 最近消息通常包含当前医生、日期、时段和待办事项，保留原文。
                keep=(
                    "messages",
                    settings.summarization_keep_messages,
                ),
                summary_prompt=CONVERSATION_SUMMARY_PROMPT,
            ),
            # 工具安全限制：单次 Agent 运行最多调用 5 次工具，防止循环调用。
            ToolCallLimitMiddleware(run_limit=5),
            # 人工确认：以下工具会修改真实业务数据，执行前必须等待患者确认。
            HumanInTheLoopMiddleware(
                interrupt_on={
                    "create_appointment": True,
                    "cancel_appointment": True,
                    "sign_in": True,
                }
            ),
        ],
        name="madp_patient_assistant",
        # 从保管箱取得 FastAPI 启动时创建的 Redis Checkpointer。
        checkpointer=get_checkpointer(),

    )

