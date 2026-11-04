package com.yilian.madp.admin.systemconfig.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 管理员保存系统业务参数的请求体。 */
public class UpdateSystemConfigRequest {

    @NotNull(message = "签到提前时间不能为空")
    @Min(value = 1, message = "签到提前时间不能小于1分钟")
    @Max(value = 180, message = "签到提前时间不能超过180分钟")
    private Integer signInAheadMinutes;

    @NotNull(message = "队列轮询间隔不能为空")
    @Min(value = 5, message = "队列轮询间隔不能小于5秒")
    @Max(value = 60, message = "队列轮询间隔不能超过60秒")
    private Integer queuePollSeconds;

    @NotNull(message = "预约取消限制不能为空")
    @Min(value = 0, message = "预约取消限制不能小于0小时")
    @Max(value = 72, message = "预约取消限制不能超过72小时")
    private Integer appointmentCancelHours;

    @NotNull(message = "退款开关不能为空")
    private Boolean refundEnabled;

    public Integer getSignInAheadMinutes() { return signInAheadMinutes; }
    public void setSignInAheadMinutes(Integer signInAheadMinutes) { this.signInAheadMinutes = signInAheadMinutes; }
    public Integer getQueuePollSeconds() { return queuePollSeconds; }
    public void setQueuePollSeconds(Integer queuePollSeconds) { this.queuePollSeconds = queuePollSeconds; }
    public Integer getAppointmentCancelHours() { return appointmentCancelHours; }
    public void setAppointmentCancelHours(Integer appointmentCancelHours) { this.appointmentCancelHours = appointmentCancelHours; }
    public Boolean getRefundEnabled() { return refundEnabled; }
    public void setRefundEnabled(Boolean refundEnabled) { this.refundEnabled = refundEnabled; }
}
