package com.yilian.madp.admin.source.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/** 调整号源时加锁读取的当前数据。 */
public record AdminSourceLockResponse(
        Long sourceId,
        Long doctorId,
        String doctorName,
        LocalDate visitDate,
        String timePeriod,
        LocalTime startTime,
        LocalTime endTime,
        Integer totalCount,
        Integer remainCount,
        Integer bookedCount,
        String status,
        String suspensionReason
) {
}
