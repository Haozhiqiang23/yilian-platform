package com.yilian.madp.appointment.vo;

/**
 * 创建预约后的精简返回结果。
 */
public class CreateAppointmentResponse {

    private final Long appointmentId;
    private final String status;

    public CreateAppointmentResponse(Long appointmentId, String status) {
        this.appointmentId = appointmentId;
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public String getStatus() {
        return status;
    }
}
