package com.yilian.madp.admin.systemconfig.vo;

import java.time.LocalDateTime;

/** 系统配置响应；字段名称与管理员前端表单保持一致。 */
public record SystemConfigResponse(
        Integer signInAheadMinutes,
        Integer queuePollSeconds,
        Integer appointmentCancelHours,
        Boolean refundEnabled,
        LocalDateTime updateTime
) {
}
