package com.yilian.madp.appointment.vo;

/**
 * 预约状态变更结果。
 */
public record AppointmentStatusResponse(
        Long appointmentId,
        String status
) {
}
