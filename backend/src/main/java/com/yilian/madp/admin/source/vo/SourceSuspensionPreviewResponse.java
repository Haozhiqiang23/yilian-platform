package com.yilian.madp.admin.source.vo;

import java.time.LocalDate;

/**
 * 号源停诊影响预览。
 *
 * <p>这里只统计影响范围，不修改任何业务数据。管理员确认影响后，
 * 再调用后续的停诊执行接口。</p>
 */
public record SourceSuspensionPreviewResponse(
        Long sourceId,
        String doctorName,
        LocalDate visitDate,
        String timePeriod,
        String sourceStatus,
        Integer totalCount,
        Integer remainCount,
        Integer affectedAppointmentCount,
        Integer pendingPaymentCount,
        Integer paidCount,
        Integer signedCount,
        Integer inConsultationCount,
        Integer refundingCount,
        Integer estimatedRefundCount,
        Boolean canSuspend,
        String blockedReason
) {
}
