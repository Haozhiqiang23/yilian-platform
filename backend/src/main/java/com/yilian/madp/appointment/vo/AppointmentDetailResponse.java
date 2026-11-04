package com.yilian.madp.appointment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 患者预约详情，组合预约、医生、号源和支付信息。
 */
public record AppointmentDetailResponse(
        Long appointmentId,
        String status,
        LocalDateTime createTime,
        Long doctorId,
        String doctorName,
        String department,
        Long sourceId,
        LocalDate visitDate,
        String timePeriod,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal fee,
        String orderNo,
        BigDecimal amount,
        String paymentStatus
) {
}
