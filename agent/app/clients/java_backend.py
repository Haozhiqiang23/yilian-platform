"""调用现有 Java 医疗后端的 HTTP 客户端。"""

import logging
from time import perf_counter
from typing import Any

import httpx

from app.core.observability import get_request_id

logger = logging.getLogger("madp.java_backend")


# 处理异常
class JavaBackendError(Exception):
    """调用 Java 后端时产生的可预期异常。"""

    # `*` 后面的参数必须通过参数名传递。
    # message 可以按位置传递，code 必须写成 code=具体值。
    def __init__(self,message:str ,*,code:int | None=None) -> None:
        super().__init__(message)
        self.message = message
        self.code = code
# client
class JavaBackendClient:
    """调用 Java 后端患者接口，并透传当前患者的 JWT。"""
    def __init__(
        self,
        base_url: str,
        timeout_seconds: float = 10.0,
        transport: httpx.AsyncBaseTransport | None = None, #测试
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout_seconds = timeout_seconds
        self._transport = transport   #测试

    @staticmethod
    def _log_call(
        *,
        method: str,
        path: str,
        started_at: float,
        status_code: int | str,
        outcome: str,
    ) -> None:
        """记录 Java 接口耗时，不记录 JWT、请求体和医疗响应数据。"""

        # 可观测性：request_id 来自 ContextVar，可把一次前端请求与 Java 调用串联。
        logger.info(
            "java_request request_id=%s method=%s path=%s status=%s "
            "duration_ms=%s outcome=%s",
            get_request_id(),
            method,
            path,
            status_code,
            round((perf_counter() - started_at) * 1000, 2),
            outcome,
        )


    """查询当前登录用户的真实身份信息。"""
    async def get_current_user(
            self,
            *,
            access_token: str,
    ) -> dict[str, Any]:
        """查询当前登录用户的真实身份信息。"""

        data = await self._get_data(
            "/users/me",
            access_token=access_token,
            fallback_message="查询当前登录用户失败，请稍后重试",
        )

        # 接口正常情况下应当返回包含 userId 和 role 的字典。
        if not isinstance(data, dict):
            raise JavaBackendError(
                "当前登录用户返回数据格式异常，请重新登录"
            )

        user_id = data.get("userId")
        role = data.get("role")

        if not isinstance(user_id, int):
            raise JavaBackendError(
                "当前登录用户编号无效，请重新登录"
            )

        if role != "patient":
            raise JavaBackendError(
                "当前智能助手仅支持患者账号使用"
            )

        return data


    # 获取后端数据并且校验的公共方法   get方法
    async def _get_data(
            self,
            path: str,
            *,
            access_token: str,
            params: dict[str, str | int] |None = None,
            fallback_message: str
                        )->Any:
        """发送带认证信息的 GET 请求，并取出统一响应中的 data。"""
        started_at = perf_counter()
        try:
            async with httpx.AsyncClient(
                    base_url=self._base_url,
                    timeout=self._timeout_seconds,
                    transport=self._transport, #测试
            ) as client:
                # response 是 Java 后端返回的完整 HTTP 响应对象，不只是业务数据。
                # 包含 HTTP状态码 响应头 后端的数据
                response = await client.get(
                    url=path,
                    params=params,
                    headers={ "Authorization": f"Bearer {access_token}"}
                )
        except httpx.TimeoutException as exc:
            self._log_call(
                method="GET", path=path, started_at=started_at,
                status_code="timeout", outcome="failed",
            )
            raise JavaBackendError( "医疗业务服务响应超时，请稍后重试") from exc
        except httpx.RequestError as exc:
            self._log_call(
                method="GET", path=path, started_at=started_at,
                status_code="connection_error", outcome="failed",
            )
            raise JavaBackendError( "暂时无法连接医疗业务服务，请稍后重试") from exc
        self._log_call(
            method="GET", path=path, started_at=started_at,
            status_code=response.status_code,
            outcome="success" if not response.is_error else "failed",
        )
        #将后端数据转换为 json格式  可以会抛异常
        try:
            #
            body = response.json()
        except ValueError as exc:
            raise JavaBackendError("医疗业务服务返回了无法识别的数据") from exc

        #处理登录失败和统一错误响应
        if response.status_code == 401:
            raise JavaBackendError(
                "登录状态已失效，请重新登录",
                code=40100,
            )

        if not isinstance(body, dict):
            raise JavaBackendError(fallback_message)
        # 处理其他错误状态码
        # HTTPX 会在 HTTP 状态码为 4xx 或 5xx 时，让 is_error 为 True
        if response.is_error or body.get("code") not in {0, 200}:
            raise JavaBackendError(
                body.get("message") or fallback_message,
                code=body.get("code"),
            )
        # 返回 只返回body 中的data
        # body
        """
                {
            "code": 200,
            "message": "查询成功",
            "data": {
                "records": []
            },
        }
        """
        return body.get("data")

    # 获取后端数据并且校验的公共方法   post方法
    async def _post_data(
        self,
        path: str,
        *,
        access_token: str,
        json: dict[str, Any],
        fallback_message: str,
    ) -> Any:
        """发送带认证信息的 POST 请求，并取出统一响应中的 data。"""

        started_at = perf_counter()

        try:
            async with httpx.AsyncClient(
                base_url=self._base_url,
                timeout=self._timeout_seconds,
                transport=self._transport,  # 测试时可以传入 MockTransport
            ) as client:
                # response 是 Java 后端返回的完整 HTTP 响应对象。
                response = await client.post(
                    url=path,
                    json=json,
                    headers={
                        "Authorization": f"Bearer {access_token}",
                    },
                )
        except httpx.TimeoutException as exc:
            self._log_call(
                method="POST", path=path, started_at=started_at,
                status_code="timeout", outcome="failed",
            )
            raise JavaBackendError(
                "医疗业务服务响应超时，请稍后重试"
            ) from exc
        except httpx.RequestError as exc:
            self._log_call(
                method="POST", path=path, started_at=started_at,
                status_code="connection_error", outcome="failed",
            )
            raise JavaBackendError(
                "暂时无法连接医疗业务服务，请稍后重试"
            ) from exc

        self._log_call(
            method="POST", path=path, started_at=started_at,
            status_code=response.status_code,
            outcome="success" if not response.is_error else "failed",
        )

        # 将 Java 后端返回的数据转换成 Python 字典。
        try:
            body = response.json()
        except ValueError as exc:
            raise JavaBackendError(
                "医疗业务服务返回了无法识别的数据"
            ) from exc

        # HTTP 401 表示患者登录状态已经失效。
        if response.status_code == 401:
            raise JavaBackendError(
                "登录状态已失效，请重新登录",
                code=40100,
            )

        if not isinstance(body, dict):
            raise JavaBackendError(fallback_message)

        # 同时判断 HTTP 状态码和 Java 后端统一业务码。
        if response.is_error or body.get("code") not in {0, 200}:
            raise JavaBackendError(
                body.get("message") or fallback_message,
                code=body.get("code"),
            )

        # Python Agent 只需要统一响应结构中的 data。
        return body.get("data")

    # 获取后端数据并且校验的公共方法   put方法
    async def _put_data(
        self,
        path: str,
        *,
        access_token: str,
        fallback_message: str,
    ) -> Any:
        """发送带认证信息的 PUT 请求，并取出统一响应中的 data。"""

        started_at = perf_counter()

        try:
            async with httpx.AsyncClient(
                base_url=self._base_url,
                timeout=self._timeout_seconds,
                transport=self._transport,  # 测试时可以传入 MockTransport
            ) as client:
                response = await client.put(
                    url=path,
                    headers={
                        "Authorization": f"Bearer {access_token}",
                    },
                )
        except httpx.TimeoutException as exc:
            self._log_call(
                method="PUT", path=path, started_at=started_at,
                status_code="timeout", outcome="failed",
            )
            raise JavaBackendError(
                "医疗业务服务响应超时，请稍后重试"
            ) from exc
        except httpx.RequestError as exc:
            self._log_call(
                method="PUT", path=path, started_at=started_at,
                status_code="connection_error", outcome="failed",
            )
            raise JavaBackendError(
                "暂时无法连接医疗业务服务，请稍后重试"
            ) from exc

        self._log_call(
            method="PUT", path=path, started_at=started_at,
            status_code=response.status_code,
            outcome="success" if not response.is_error else "failed",
        )

        try:
            body = response.json()
        except ValueError as exc:
            raise JavaBackendError(
                "医疗业务服务返回了无法识别的数据"
            ) from exc

        if response.status_code == 401:
            raise JavaBackendError(
                "登录状态已失效，请重新登录",
                code=40100,
            )

        if not isinstance(body, dict):
            raise JavaBackendError(fallback_message)

        if response.is_error or body.get("code") not in {0, 200}:
            raise JavaBackendError(
                body.get("message") or fallback_message,
                code=body.get("code"),
            )

        return body.get("data")




    #预约
    async def get_my_appointments(
        self,
        *,
        access_token: str,
        status: str | None = None,
        page: int = 1,
        size: int = 10,
    ) -> dict[str, Any]:
        """查询当前登录患者自己的预约列表。"""

        params: dict[str, str | int] = {
            "page": page,
            "size": size,
        }

        if status:
            params["status"] = status

        # 调用定义好的 公共的get方法
        data =await self._get_data(
            "/appointments/my",
            access_token=access_token,
            params=params,
            fallback_message="预约查询失败，请稍后重试"
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "预约数据格式异常，请稍后重试"
            )

        return data

    #查询医生
    async def search_doctors(
            self,*,
            access_token: str,
            department_id: int | None = None,
            name: str | None = None,
            date: str | None = None,
            page: int = 1,
            size: int = 10,
    )-> dict[str, Any]:
        # 参数处理
        params: dict[str, str | int] = {
            "page": page,
            "size": size,
        }
        # 校验
        if department_id is not None:
            params["departmentId"] = department_id
        if name:
            params["name"] = name
        if date:
            params["date"] = date

        # 调用定义好的 公共的get方法
        data =await self._get_data(
            "/doctors",
            access_token=access_token,
            params=params,
            fallback_message="查询医生失败，请稍后重试",
        )
        #
        if not isinstance(data, dict):
            raise JavaBackendError(
                "医生列表数据格式异常，请稍后重试"
            )

        return data

    # 查询医生详情
    async def get_doctor_detail(
        self,
        *,
        access_token: str,
        doctor_id: int,
    ) -> dict[str, Any]:
        """根据医生 ID 查询医生详情。"""
        data = await self._get_data(
            f"/doctors/{doctor_id}",
            access_token=access_token,
            fallback_message="查询医生详情失败，请稍后重试",
        )
        if not isinstance(data, dict):
            raise JavaBackendError(
                "医生详情数据格式异常，请稍后重试"
            )

        return data


    # 查询指定医生的预约号源。
    async def search_appointment_sources(
        self,
        *,
        access_token: str,
        doctor_id: int,
        date: str | None = None,
    ) -> list[dict[str, Any]]:
        """查询指定医生的预约号源。"""
        params: dict[str, str | int] = {
            "doctorId": doctor_id,
        }

        if date:
            params["date"] = date

        data = await self._get_data(
            "/sources",
            access_token=access_token,
            params=params,
            fallback_message="查询号源失败，请稍后重试",
        )

        if not isinstance(data, list) or not all(
            isinstance(item, dict) for item in data
        ):
            raise JavaBackendError(
                "号源数据格式异常，请稍后重试"
            )

        return data


    #查询我的预约
    async def get_appointment_detail(
        self,
        *,
        access_token: str,
        appointment_id: int,
    ) -> dict[str, Any]:
        """查询当前患者自己的预约详情。"""
        data = await self._get_data(
            f"/appointments/{appointment_id}",
            access_token=access_token,
            fallback_message="查询预约详情失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "预约详情数据格式异常，请稍后重试"
            )

        return data


    # 查询当前患者的排队状态
    async def get_queue_status(
        self,
        *,
        access_token: str,
        appointment_id: int,
    ) -> dict[str, Any]:
        """根据预约编号查询当前患者的排队状态。"""
        data = await self._get_data(
            f"/queues/{appointment_id}",
            access_token=access_token,
            fallback_message="查询排队进度失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "排队状态数据格式异常，请稍后重试"
            )

        return data

    #我的诊疗记录
    async def get_my_medical_records(
        self,
        *,
        access_token: str,
        page: int = 1,
        size: int = 10,
    ) -> dict[str, Any]:
        """分页查询当前患者自己的诊疗记录。"""

        params: dict[str, str | int] = {
            "page": page,
            "size": size,
        }
        data = await self._get_data(
            "/medical-records/my",
            access_token=access_token,
            params=params,
            fallback_message="查询诊疗记录失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "诊疗记录数据格式异常，请稍后重试"
            )

        return data

    # 我的检查申请
    async def get_my_examinations(
        self,
        *,
        access_token: str,
    ) -> list[dict[str, Any]]:
        """查询当前患者自己的全部检查申请。"""
        data = await self._get_data(
            "/examinations/my",
            access_token=access_token,
            fallback_message="查询检查申请失败，请稍后重试",
        )

        if not isinstance(data, list) or not all(
            isinstance(item, dict) for item in data
        ):
            raise JavaBackendError(
                "检查申请数据格式异常，请稍后重试"
            )

        return data

    # 检查报告详情
    async def get_examination_report(
        self,
        *,
        access_token: str,
        examination_id: int,
    ) -> dict[str, Any]:
        """根据检查申请编号查询当前患者自己的正式检查报告。"""
        data = await self._get_data(
            f"/examinations/{examination_id}/report",
            access_token=access_token,
            fallback_message="查询检查报告失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "检查报告数据格式异常，请稍后重试"
            )

        return data

    # 处方详情
    async def get_prescription_detail(
        self,
        *,
        access_token: str,
        prescription_id: int,
    ) -> dict[str, Any]:
        """根据处方编号查询当前患者自己的处方详情。"""
        data = await self._get_data(
            f"/prescriptions/{prescription_id}",
            access_token=access_token,
            fallback_message="查询处方详情失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "处方详情数据格式异常，请稍后重试"
            )

        return data

    # 我的消息列表
    async def get_my_messages(
        self,
        *,
        access_token: str,
        message_type: str | None = None,
        unread_only: bool = False,
        page: int = 1,
        size: int = 10,
    ) -> dict[str, Any]:
        """分页查询当前患者自己的站内消息。"""
        params: dict[str, str | int] = {
            "unreadOnly": str(unread_only).lower(),
            "page": page,
            "size": size,
        }

        if message_type:
            params["type"] = message_type

        data = await self._get_data(
            "/messages",
            access_token=access_token,
            params=params,
            fallback_message="查询消息失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "消息列表数据格式异常，请稍后重试"
            )

        return data

    # 消息详情
    async def get_message_detail(
        self,
        *,
        access_token: str,
        message_id: int,
    ) -> dict[str, Any]:
        """根据消息编号查询当前患者自己的消息详情。"""
        data = await self._get_data(
            f"/messages/{message_id}",
            access_token=access_token,
            fallback_message="查询消息详情失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "消息详情数据格式异常，请稍后重试"
            )

        return data

    # 预约支付详情
    async def get_payment_by_appointment(
        self,
        *,
        access_token: str,
        appointment_id: int,
    ) -> dict[str, Any]:
        """根据预约编号查询当前患者自己的支付订单详情。"""
        data = await self._get_data(
            f"/payments/by-appointment/{appointment_id}",
            access_token=access_token,
            fallback_message="查询支付详情失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "支付详情数据格式异常，请稍后重试"
            )

        return data

    # 人工确认


    # 创建预约
    async def create_appointment(
        self,
        *,
        access_token: str,
        source_id: int,
    ) -> dict[str, Any]:
        """使用当前登录患者和指定号源创建预约。"""

        # 患者身份由 JWT 确定，请求体只需要传递号源编号。
        data = await self._post_data(
            "/appointments",
            access_token=access_token,
            json={
                "sourceId": source_id,
            },
            fallback_message="创建预约失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "创建预约返回数据格式异常，请稍后重试"
            )

        return data

    # 取消预约
    async def cancel_appointment(
        self,
        *,
        access_token: str,
        appointment_id: int,
    ) -> dict[str, Any]:
        """取消当前登录患者自己的指定预约。"""

        data = await self._put_data(
            f"/appointments/{appointment_id}/cancel",
            access_token=access_token,
            fallback_message="取消预约失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "取消预约返回数据格式异常，请稍后重试"
            )

        return data

    # 预约签到
    async def sign_in(
        self,
        *,
        access_token: str,
        appointment_id: int,
    ) -> dict[str, Any]:
        """使用当前登录患者和指定预约完成签到。"""

        data = await self._post_data(
            "/sign-ins",
            access_token=access_token,
            json={
                "appointmentId": appointment_id,
            },
            fallback_message="签到失败，请稍后重试",
        )

        if not isinstance(data, dict):
            raise JavaBackendError(
                "签到返回数据格式异常，请稍后重试"
            )

        return data








