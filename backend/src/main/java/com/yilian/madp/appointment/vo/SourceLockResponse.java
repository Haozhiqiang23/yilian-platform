package com.yilian.madp.appointment.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建预约时锁定读取的号源关键字段。
 */
public record SourceLockResponse(
        Long id,
        Long doctorId,
        Integer remainCount,
        String status,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
