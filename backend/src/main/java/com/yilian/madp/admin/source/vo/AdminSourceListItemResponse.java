package com.yilian.madp.admin.source.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/** 管理员号源列表项，同时返回医生和科室信息。 */
public record AdminSourceListItemResponse(
        Long sourceId,
        Long doctorId,
        String doctorName,
        String department,
        LocalDate visitDate,
        String timePeriod,
        LocalTime startTime,
        LocalTime endTime,
        Integer totalCount,
        Integer remainCount,
        Integer bookedCount,
        String status
) {
}
