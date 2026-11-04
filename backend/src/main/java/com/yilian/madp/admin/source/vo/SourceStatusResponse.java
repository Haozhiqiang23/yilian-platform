package com.yilian.madp.admin.source.vo;

/** 号源停诊或恢复后的状态。 */
public record SourceStatusResponse(
        Long sourceId,
        String status,
        String reason,
        Integer processedAppointmentCount
) {
}
