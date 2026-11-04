package com.yilian.madp.appointment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * “我的预约”列表中的单条数据。
 */
public record AppointmentListItemResponse(
        Long appointmentId,
        String status,
        Long doctorId,
        String doctorName,
        String department,
        LocalDate visitDate,
        String timePeriod,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal fee,
        BigDecimal amount,
        LocalDateTime createTime
) {
}
