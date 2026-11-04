package com.yilian.madp.common;
//错误代码枚举类
public enum ErrorCode {
    BAD_REQUEST(400, "请求参数不合法"),

    UNAUTHORIZED(401, "未登录或登录已过期"),

    FORBIDDEN(403, "无权限访问"),

    NOT_FOUND(404, "请求资源不存在"),

    PATIENT_PROFILE_NOT_FOUND(404, "患者个人信息不存在"),

    DOCTOR_NOT_FOUND(404, "医生不存在或已停用"),

    DOCTOR_AUDIT_STATUS_ERROR(409, "当前医生状态不允许审核"),

    SOURCE_NOT_FOUND(404, "号源不存在"),

    NOT_PATIENT(403, "当前账号不是患者"),

    SOURCE_NOT_ENOUGH(409, "号源已约满"),

    SOURCE_EXPIRED(409, "已结束的号源不能调整"),

    SOURCE_APPOINTMENT_EXPIRED(409, "已结束的号源不能预约"),

    SOURCE_CREATE_EXPIRED(409, "不能新建已经结束的号源"),

    SOURCE_TOTAL_TOO_SMALL(409, "总号数不能小于已预约数量"),

    SOURCE_TIME_CONFLICT(409, "该医生在相同日期和时段已存在号源"),
    SOURCE_TIME_INVALID(400, "出诊开始时间必须早于结束时间"),

    SOURCE_HAS_APPOINTMENTS(409, "已有患者预约，不能调整出诊日期或时段"),

    SOURCE_ADJUST_REASON_REQUIRED(400, "调整出诊日期或时段时必须填写原因"),

    SOURCE_SUSPENDED(409, "该号源已停诊，不能预约"),

    SOURCE_STATUS_ERROR(409, "当前号源状态不允许此操作"),

    SOURCE_ACTIVE_APPOINTMENTS_EXIST(409, "该号源存在有效预约，暂不能直接停诊"),

    SOURCE_SUSPENSION_REASON_REQUIRED(400, "停诊时必须填写原因"),

    APPOINTMENT_ALREADY_EXISTS(409, "同一时间段已存在有效预约"),

    APPOINTMENT_NOT_FOUND(404, "预约不存在"),

    APPOINTMENT_STATUS_ERROR(409, "当前预约状态不允许此操作"),

    APPOINTMENT_CANCEL_DEADLINE(409, "已超过预约可取消时间，请联系医院工作人员"),

    PAYMENT_NOT_REQUIRED(409, "当前预约状态不允许创建支付订单"),

    PAYMENT_EXIST(409, "该预约已存在支付订单"),

    PAYMENT_NOT_FOUND(404, "支付订单不存在"),

    PAYMENT_STATUS_ERROR(409, "支付订单状态异常"),

    REFUND_NOT_FOUND(404, "退款申请不存在"),

    REFUND_STATUS_ERROR(409, "当前退款状态不允许处理"),

    REFUND_DISABLED(409, "退款申请入口已关闭，请联系医院工作人员"),

    SIGN_TIME_ERROR(409, "当前时间不在签到范围内，请按允许时间签到"),

    SIGN_ALREADY_EXISTS(409, "该预约已经完成签到"),

    APPOINTMENT_NOT_PAY(409, "预约尚未支付，不能签到"),

    QUEUE_NOT_FOUND(404, "排队记录不存在"),

    QUEUE_STATUS_ERROR(409, "只有等待接诊的队列可以调整顺序"),

    QUEUE_DATE_ERROR(409, "只能调整当天的等待队列"),

    QUEUE_POSITION_ERROR(400, "目标位置超出当前等待队列范围"),

    QUEUE_POSITION_UNCHANGED(409, "目标位置与当前位置相同"),

    OPERATION_LOG_NOT_FOUND(404, "操作日志不存在"),

    SYSTEM_CONFIG_NOT_FOUND(404, "系统配置不存在，请先初始化配置表"),

    MEDICAL_RECORD_NOT_FOUND(404, "诊疗记录不存在"),

    MEDICAL_RECORD_EXIST(409, "该预约已存在诊疗记录"),

    PRESCRIPTION_NOT_FOUND(404, "处方不存在"),

    PRESCRIPTION_STATUS_ERROR(409, "当前处方状态不允许修改"),

    REPORT_NOT_FOUND(404, "检查报告不存在"),

    EXAMINATION_NOT_FOUND(404, "检查申请不存在"),

    EXAMINATION_STATUS_ERROR(409, "当前检查状态不允许此操作"),

    EXAMINATION_REPORT_EXIST(409, "该检查申请已存在报告"),

    MESSAGE_NOT_FOUND(404, "消息不存在"),

    VERIFY_CODE_INVALID(400, "验证码错误或已过期"),

    PHONE_ALREADY_REGISTERED(409, "手机号已注册"),

    ID_CARD_ALREADY_REGISTERED(409, "身份证号已注册"),

    USER_NOT_FOUND(404, "用户不存在"),

    CANNOT_DISABLE_SELF(409, "管理员不能禁用自己的账号"),

    CONFLICT(409, "请求发生冲突"),

    INTERNAL_ERROR(500, "服务器内部错误");


    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
