import logging
from datetime import datetime
from functools import lru_cache
from zoneinfo import ZoneInfo

from langchain_core.exceptions import ModelError
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage, ToolMessage
from langchain_openai.chat_models.base import OpenAIPermissionDeniedError
from langgraph.types import Command
from openai import (
    APIConnectionError,
    APITimeoutError,
    OpenAIError,
    RateLimitError,
)

from app.agent.context import AgentContext
from app.agent.factory import get_patient_agent
from app.clients.dependencies import get_java_backend_client
from app.clients.java_backend import JavaBackendError
from app.core.chat_lock import get_chat_lock_manager
from app.core.exceptions import AppException
from app.core.observability import get_request_id
from app.schemas.chat import ChatHistoryMessage, ChatHistoryResponse, ChatResponse

logger = logging.getLogger("madp.chat")


class ChatService:
    """组织患者与智能助手之间的对话。"""

    def __init__(self)->None:
        self._agent=get_patient_agent()

    """读取当前患者指定 Redis 会话中的可见聊天记录。"""
    async def get_history(
        self,
        *,
        access_token: str,
        session_id: str,
    ) -> ChatHistoryResponse:
        """读取当前患者指定 Redis 会话中的可见聊天记录。"""

        # Redis 会话历史：仍然使用真实 userId 构建 thread_id，
        # 防止患者通过伪造 session_id 读取其他患者的聊天记录。
        thread_id = await self._build_thread_id(
            access_token=access_token,
            session_id=session_id,
        )

        snapshot = await self._agent.aget_state(
            {
                "configurable": {
                    "thread_id": thread_id,
                }
            }
        )
        state_messages = snapshot.values.get("messages", [])
        history_messages: list[ChatHistoryMessage] = []

        for current_message in state_messages:
            if isinstance(current_message, HumanMessage):
                # 上下文控制（第二层）：摘要消息只用于给模型恢复早期上下文，
                # 不能伪装成患者输入显示在聊天历史中。
                if current_message.additional_kwargs.get("lc_source") == "summarization":
                    continue
                content = self._visible_human_content(current_message.text)
                role = "user"
            elif isinstance(current_message, AIMessage):
                # Redis 会话历史：模型准备调用工具时可能生成空 AIMessage，
                # 这类内部消息不应显示在患者聊天窗口中。
                if current_message.tool_calls:
                    continue
                content = self._sanitize_assistant_content(current_message.text)
                role = "assistant"
            else:
                # ToolMessage 等内部执行消息只供 Agent 恢复状态使用。
                continue

            if not content:
                continue

            history_messages.append(
                ChatHistoryMessage(
                    role=role,
                    content=content,
                )
            )

        return ChatHistoryResponse(
            session_id=session_id,
            messages=history_messages,
            # 待确认恢复：刷新页面后重新展示预约、取消或签到确认按钮。
            pending_confirmation=self._get_pending_confirmation(snapshot),
        )

    @staticmethod
    def _visible_human_content(content: str) -> str:
        """移除发送给模型的日期说明，只保留患者真正输入的内容。"""

        marker = "\n患者消息："
        if marker in content:
            return content.split(marker, maxsplit=1)[1].strip()

        return content.strip()

    @staticmethod
    def _sanitize_assistant_content(content: str) -> str:
        """清理页面不需要展示的 Markdown 装饰字符。"""

        # 【RAG回答格式修复】模型偶尔会忽略提示词输出 Markdown 加粗或标题。
        # 统一出口再次清理，保证普通回复、流式回复和历史记录展示一致。
        cleaned_lines = []
        for line in content.replace("*", "").splitlines():
            cleaned_lines.append(line.lstrip("# ") if line.startswith("#") else line)

        return "\n".join(cleaned_lines).strip()

    def _get_pending_confirmation(self, snapshot: object) -> ChatResponse | None:
        """从 LangGraph 暂停状态中恢复患者尚未处理的确认操作。"""

        # 待确认恢复：中断信息保存在 StateSnapshot 的 task.interrupts 中。
        for task in getattr(snapshot, "tasks", ()):
            for interrupt in getattr(task, "interrupts", ()):
                interrupt_value = getattr(interrupt, "value", None)
                if not isinstance(interrupt_value, dict):
                    continue

                action_requests = interrupt_value.get("action_requests", [])
                if not action_requests:
                    continue

                pending_action = action_requests[0]
                if not isinstance(pending_action, dict):
                    continue

                return ChatResponse(
                    content=self._confirmation_content(pending_action),
                    requires_confirmation=True,
                    pending_action=pending_action,
                )

        return None

    @staticmethod
    def _confirmation_content(pending_action: dict | None) -> str:
        """根据待执行工具生成患者可理解的确认说明。"""

        pending_action = pending_action or {}
        action_name = pending_action.get("name")
        action_args = pending_action.get("args", {})

        if action_name == "cancel_appointment":
            appointment_id = action_args.get("appointment_id")
            return (
                f"即将取消预约编号 {appointment_id}，请确认是否继续。"
                if appointment_id is not None
                else "即将取消预约，请确认是否继续。"
            )

        if action_name == "sign_in":
            appointment_id = action_args.get("appointment_id")
            return (
                f"即将为预约编号 {appointment_id} 办理签到并进入排队，"
                "请确认是否继续。"
                if appointment_id is not None
                else "即将办理签到并进入排队，请确认是否继续。"
            )

        source_id = action_args.get("source_id")
        return (
            f"即将使用号源编号 {source_id} 创建预约，请确认是否继续。"
            if source_id is not None
            else "即将创建预约，请确认是否继续。"
        )


    async def chat(
        self,
        message: str | None,
        access_token: str,
        session_id: str,
        decision: str | None,
    ) -> ChatResponse:
      # 会话并发锁：普通消息和人工确认共用同一把 thread_id 锁，
      # 防止回答未结束时重复发送，或确认按钮被连续点击两次。
      thread_id = await self._build_thread_id(
          access_token=access_token,
          session_id=session_id,
      )
      # 可观测性：thread_id 已由 Java 验证后的 userId 生成，
      # 日志不记录 JWT、患者消息正文或模型完整回答。
      logger.info(
          "chat_started request_id=%s thread_id=%s mode=standard decision=%s",
          get_request_id(),
          thread_id,
          decision or "message",
      )
      async with get_chat_lock_manager().lock(thread_id):
          return await self._chat_unlocked(
              message=message,
              access_token=access_token,
              decision=decision,
              thread_id=thread_id,
          )

    async def _chat_unlocked(
        self,
        *,
        message: str | None,
        access_token: str,
        decision: str | None,
        thread_id: str,
    ) -> ChatResponse:
      # Redis 会话隔离：前端 session_id 不能单独作为 Redis 会话标识。
      # thread_id 已由公开 chat() 方法验证 JWT 后生成。

      # 每次请求时获取北京时间，避免模型错误理解“今天”“明天”和未写年份的日期。
      current_date = datetime.now(
          ZoneInfo("Asia/Shanghai")
      ).date().isoformat()

      if decision is not None:
          # 使用相同的 thread_id，从 LangGraph 上一次暂停的位置继续执行。
          agent_input = Command(
              resume={
                  "decisions": [
                      {
                          "type": decision,
                      }
                  ]
              }
          )
      else:
          # ChatRequest 已经保证普通消息不为空。
          agent_input = {
              "messages": [
                  {
                      "role": "user",
                      "content": (
                          f"系统当前日期（北京时间）：{current_date}\n"
                          f"患者消息：{message}"
                      ),
                  }
              ]
          }

      #本次发给模型的患者消息
      result = await self._invoke_agent(
          agent_input=agent_input,
          access_token=access_token,
          thread_id=thread_id,
      )

      # 预约写操作执行前会被人工确认中间件暂停。
      interrupts = result.get("__interrupt__", [])
      if interrupts:
          interrupt_value = interrupts[0].value
          action_requests = interrupt_value.get("action_requests", [])
          pending_action = action_requests[0] if action_requests else None

          action_name = None
          action_args = {}
          if pending_action:
              action_name = pending_action.get("name")
              action_args = pending_action.get("args", {})

          if action_name == "cancel_appointment":
              confirmation_content = self._confirmation_content(pending_action)
          elif action_name == "sign_in":
              appointment_id = action_args.get("appointment_id")

              # 人工确认中间件会在工具执行前暂停，因此在展示确认按钮前，
              # 先查询真实预约详情。只有已支付预约才允许患者继续签到。
              try:
                  appointment = await get_java_backend_client().get_appointment_detail(
                      access_token=access_token,
                      appointment_id=appointment_id,
                  )
              except JavaBackendError as exc:
                  # 拒绝本次待执行工具，清除 LangGraph 中的暂停状态。
                  await self._reject_pending_action(
                      access_token=access_token,
                      thread_id=thread_id,
                  )
                  return ChatResponse(content=exc.message)

              appointment_status = appointment.get("status")
              if appointment_status != "已支付":
                  # 状态不满足时自动拒绝，不把无效签到留在待确认状态。
                  await self._reject_pending_action(
                      access_token=access_token,
                      thread_id=thread_id,
                  )
                  return ChatResponse(
                      content=(
                          f"预约编号 {appointment_id} 当前状态为“{appointment_status}”，"
                          "只有已支付预约可以办理签到。"
                      )
                  )

              confirmation_content = self._confirmation_content(pending_action)
          else:
              confirmation_content = self._confirmation_content(pending_action)

          return ChatResponse(
              content=confirmation_content,
              requires_confirmation=True,
              pending_action=pending_action,
          )

      # 校验异常
      messages = result.get("messages", [])
      if not messages:
          raise AppException(
              "智能助手未返回消息",
              code=50202,
              status_code=502,
          )

      last_message = messages[-1]
      if not isinstance(last_message, AIMessage):
          raise AppException(
              "智能助手返回数据异常",
              code=50202,
              status_code=502,
          )

      content = self._sanitize_assistant_content(last_message.text)
      if not content:
          raise AppException(
              "智能助手未返回有效内容",
              code=50203,
              status_code=502,
          )

      action = None
      # 只检查本轮用户消息之后产生的工具结果，避免后续回答重复显示支付按钮。
      for current_message in reversed(messages):
          # 只检查本轮用户消息之后产生的工具结果，避免后续回答重复显示支付按钮。
          if isinstance(current_message, HumanMessage):
              break
          if not isinstance(current_message, ToolMessage):
              continue
          if not isinstance(current_message.artifact, dict):
              continue
          if current_message.artifact.get("success") is not True:
              continue

          if current_message.name == "sign_in":
              appointment_id = current_message.artifact.get("appointment_id")
              if appointment_id is None:
                  continue

              action = {
                  "type": "VIEW_QUEUE",
                  "appointment_id": appointment_id,
                  "path": f"/patient/queue/{appointment_id}",
              }
              break

          appointment = current_message.artifact.get("appointment")
          if not isinstance(appointment, dict):
              continue

          appointment_id = appointment.get("appointmentId")
          if appointment_id is None:
              continue

          if current_message.name == "create_appointment":
              action = {
                  "type": "GO_TO_PAYMENT",
                  "appointment_id": appointment_id,
                  "path": f"/patient/appointments/{appointment_id}/pay",
              }
              break

          if current_message.name == "cancel_appointment":
              action = {
                  "type": "VIEW_APPOINTMENTS",
                  "appointment_id": appointment_id,
                  "path": "/patient/appointments",
              }
              break

      return ChatResponse(
          content=content,
          action=action,
      )

    """流式执行普通消息，并逐步返回前端可以识别的事件。"""
    async def stream_chat(
        self,
        *,
        message: str,
        access_token: str,
        session_id: str,
    ):
        """流式执行普通消息，并逐步返回前端可以识别的事件。"""

        thread_id = await self._build_thread_id(
            access_token=access_token,
            session_id=session_id,
        )
        logger.info(
            "chat_started request_id=%s thread_id=%s mode=stream decision=message",
            get_request_id(),
            thread_id,
        )
        # 会话并发锁：锁会保持到流式生成和最终状态读取全部结束。
        async with get_chat_lock_manager().lock(thread_id):
            async for event in self._stream_chat_unlocked(
                message=message,
                access_token=access_token,
                session_id=session_id,
                thread_id=thread_id,
            ):
                yield event

    async def _stream_chat_unlocked(
        self,
        *,
        message: str,
        access_token: str,
        session_id: str,
        thread_id: str,
    ):
        """持有会话锁后执行真正的 SSE Agent 流程。"""

        # SSE 流式输出：第一阶段只处理普通患者消息。
        # approve/reject 仍走原有 chat()，避免改变已经验证的人工确认恢复流程。
        current_date = datetime.now(
            ZoneInfo("Asia/Shanghai")
        ).date().isoformat()
        agent_input = {
            "messages": [
                {
                    "role": "user",
                    "content": (
                        f"系统当前日期（北京时间）：{current_date}\n"
                        f"患者消息：{message}"
                    ),
                }
            ]
        }
        config = {"configurable": {"thread_id": thread_id}}

        yield {
            "event": "start",
            "data": {"session_id": session_id},
        }

        try:
            async for message_chunk, metadata in self._agent.astream(
                agent_input,
                config=config,
                context=AgentContext(access_token=access_token),
                stream_mode="messages",
            ):
                # SSE 流式输出：只展示主模型的自然语言。
                # 工具参数和长对话摘要属于 Agent 内部数据，不能发送到页面。
                if not isinstance(message_chunk, AIMessageChunk):
                    continue
                if message_chunk.tool_call_chunks:
                    continue
                if metadata.get("lc_source") == "summarization":
                    continue

                # 【RAG回答格式修复】流式 token 也要清理，避免页面先看到星号，
                # 等 done 事件到达后才突然变成无 Markdown 的最终文本。
                chunk_content = message_chunk.text.replace("*", "")
                if chunk_content:
                    yield {
                        "event": "token",
                        "data": {"content": chunk_content},
                    }

            # SSE 流式输出：结束后读取 Redis 中的最终状态，
            # 识别确认卡片以及支付、排队等结构化动作。
            snapshot = await self._agent.aget_state(config)
            pending_response = self._get_pending_confirmation(snapshot)
            if pending_response is not None:
                pending_response = await self._validate_stream_confirmation(
                    response=pending_response,
                    access_token=access_token,
                    thread_id=thread_id,
                )
                yield {
                    "event": "done",
                    "data": pending_response.model_dump(),
                }
                return

            response = self._build_stream_response(
                snapshot.values.get("messages", [])
            )
            yield {
                "event": "done",
                "data": response.model_dump(),
            }
        except AppException as exc:
            # SSE 建立后不能再由 FastAPI 全局异常处理器修改 HTTP 响应，
            # 因此把项目异常转换成前端能够读取的 error 事件。
            yield self._stream_error(exc.code, exc.message)
        except OpenAIPermissionDeniedError:
            yield self._stream_error(50301, "模型服务额度已用完，请联系管理员")
        except APITimeoutError:
            yield self._stream_error(50401, "回答生成超时，请稍后重试")
        except APIConnectionError:
            yield self._stream_error(50302, "暂时无法连接模型服务，请稍后重试")
        except RateLimitError:
            yield self._stream_error(50303, "模型服务请求过于频繁，请稍后重试")
        except (OpenAIError, ModelError):
            yield self._stream_error(50300, "智能助手模型服务暂时不可用，请稍后重试")

    """创建统一的 SSE 错误事件。"""
    @staticmethod
    def _stream_error(code: int, message: str) -> dict:
        """创建统一的 SSE 错误事件。"""

        return {
            "event": "error",
            "data": {
                "code": code,
                "message": message,
            },
        }

    """在流式请求中校验即将展示的签到确认。"""
    async def _validate_stream_confirmation(
        self,
        *,
        response: ChatResponse,
        access_token: str,
        thread_id: str,
    ) -> ChatResponse:
        """在流式请求中校验即将展示的签到确认。"""

        pending_action = response.pending_action or {}
        if pending_action.get("name") != "sign_in":
            return response

        appointment_id = pending_action.get("args", {}).get("appointment_id")
        try:
            appointment = await get_java_backend_client().get_appointment_detail(
                access_token=access_token,
                appointment_id=appointment_id,
            )
        except JavaBackendError as exc:
            await self._reject_pending_action(
                access_token=access_token,
                thread_id=thread_id,
            )
            return ChatResponse(content=exc.message)

        appointment_status = appointment.get("status")
        if appointment_status == "已支付":
            return response

        await self._reject_pending_action(
            access_token=access_token,
            thread_id=thread_id,
        )
        return ChatResponse(
            content=(
                f"预约编号 {appointment_id} 当前状态为“{appointment_status}”，"
                "只有已支付预约可以办理签到。"
            )
        )

    """从 Agent 最终状态构造与原聊天接口一致的响应。"""
    def _build_stream_response(self, messages: list) -> ChatResponse:
        """从 Agent 最终状态构造与原聊天接口一致的响应。"""

        if not messages:
            raise AppException(
                "智能助手未返回消息",
                code=50202,
                status_code=502,
            )

        last_message = messages[-1]
        if not isinstance(last_message, AIMessage):
            raise AppException(
                "智能助手返回数据异常",
                code=50202,
                status_code=502,
            )

        content = self._sanitize_assistant_content(last_message.text)
        if not content:
            raise AppException(
                "智能助手未返回有效内容",
                code=50203,
                status_code=502,
            )

        action = None
        for current_message in reversed(messages):
            if isinstance(current_message, HumanMessage):
                break
            if not isinstance(current_message, ToolMessage):
                continue
            if not isinstance(current_message.artifact, dict):
                continue
            if current_message.artifact.get("success") is not True:
                continue

            if current_message.name == "sign_in":
                appointment_id = current_message.artifact.get("appointment_id")
                if appointment_id is not None:
                    action = {
                        "type": "VIEW_QUEUE",
                        "appointment_id": appointment_id,
                        "path": f"/patient/queue/{appointment_id}",
                    }
                    break

            appointment = current_message.artifact.get("appointment")
            if not isinstance(appointment, dict):
                continue
            appointment_id = appointment.get("appointmentId")
            if appointment_id is None:
                continue

            if current_message.name == "create_appointment":
                action = {
                    "type": "GO_TO_PAYMENT",
                    "appointment_id": appointment_id,
                    "path": f"/patient/appointments/{appointment_id}/pay",
                }
                break
            if current_message.name == "cancel_appointment":
                action = {
                    "type": "VIEW_APPOINTMENTS",
                    "appointment_id": appointment_id,
                    "path": "/patient/appointments",
                }
                break

        return ChatResponse(content=content, action=action)

    """使用真实患者编号和前端会话编号构建 Redis thread_id。"""
    async def _build_thread_id(
        self,
        *,
        access_token: str,
        session_id: str,
    ) -> str:
        """使用真实患者编号和前端会话编号构建 Redis thread_id。"""

        # Redis 会话隔离：userId 必须来自 Java 后端验证后的 JWT，
        # 不能相信前端自行传递的患者编号。
        try:
            current_user = await get_java_backend_client().get_current_user(
                access_token=access_token
            )
        except JavaBackendError as exc:
            status_code = 401 if exc.code == 40100 else 502

            raise AppException(
                exc.message,
                code=exc.code or 50210,
                status_code=status_code,
            ) from exc

        user_id = current_user["userId"]

        # Redis 会话隔离：相同 session_id 在不同患者账号下会得到不同 thread_id。
        return f"patient:{user_id}:{session_id}"

    """拒绝当前待确认工具，清除 LangGraph 中的暂停状态。"""
    async def _reject_pending_action(
        self,
        *,
        access_token: str,
        thread_id: str,
    ) -> None:
        """拒绝当前待确认工具，清除 LangGraph 中的暂停状态。"""

        await self._invoke_agent(
            agent_input=Command(
                resume={
                    "decisions": [
                        {
                            "type": "reject",
                        }
                    ]
                }
            ),
            access_token=access_token,
            thread_id=thread_id,
        )

    """调用 Agent，并把模型 SDK 异常转换为项目统一业务异常。"""
    async def _invoke_agent(
        self,
        *,
        agent_input: object,
        access_token: str,
        thread_id: str,
    ) -> dict:
        """调用 Agent，并把模型 SDK 异常转换为项目统一业务异常。"""

        try:
            return await self._agent.ainvoke(
                agent_input,
                # Redis 会话隔离：thread_id 同时包含真实患者编号和前端会话编号。
                config={
                    "configurable": {
                        "thread_id": thread_id,
                    }
                },
                # JWT 放入运行上下文，工具调用 Java 后端时从这里读取。
                context=AgentContext(
                    access_token=access_token
                ),
            )
        except OpenAIPermissionDeniedError as exc:
            raise AppException(
                "模型服务额度已用完，请联系管理员",
                code=50301,
                status_code=503,
            ) from exc
        except APITimeoutError as exc:
            raise AppException(
                "回答生成超时，请稍后重试",
                code=50401,
                status_code=504,
            ) from exc
        except APIConnectionError as exc:
            raise AppException(
                "暂时无法连接模型服务，请稍后重试",
                code=50302,
                status_code=503,
            ) from exc
        except RateLimitError as exc:
            raise AppException(
                "模型服务请求过于频繁，请稍后重试",
                code=50303,
                status_code=503,
            ) from exc
        except (OpenAIError, ModelError) as exc:
            raise AppException(
                "智能助手模型服务暂时不可用，请稍后重试",
                code=50300,
                status_code=503,
            ) from exc

@lru_cache
def get_chat_service() -> ChatService:
    return ChatService()
