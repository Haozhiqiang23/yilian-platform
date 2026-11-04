package com.yilian.madp.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 创建预约请求。
 */
public class CreateAppointmentRequest {

    @NotNull(message = "号源不能为空")
    @Positive(message = "号源ID必须大于0")
    private Long sourceId;

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
}
