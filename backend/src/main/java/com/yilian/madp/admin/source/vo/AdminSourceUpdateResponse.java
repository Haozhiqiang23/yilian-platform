package com.yilian.madp.admin.source.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/** 号源调整成功后的最新数据。 */
public record AdminSourceUpdateResponse(
        Long sourceId,
        LocalDate visitDate,
        String timePeriod,
        LocalTime startTime,
        LocalTime endTime,
        Integer totalCount,
        Integer remainCount
) {
}
