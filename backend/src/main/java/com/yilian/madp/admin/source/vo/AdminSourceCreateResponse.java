package com.yilian.madp.admin.source.vo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** 新建号源结果。 */
public record AdminSourceCreateResponse(
        Long sourceId,
        Long doctorId,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer totalCount,
        BigDecimal fee
) {
}
