package com.yilian.madp.source.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

/**
 * 返回给前端的号源信息。
 */
@Data
public class SourceResponse {

    private Long sourceId;

    private Long doctorId;

    private LocalDate visitDate;

    /** 兼容旧前端的时段字段，暂不删除。 */
    private String timePeriod;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal fee;

    private Integer totalCount;

    private Integer remainCount;
}
