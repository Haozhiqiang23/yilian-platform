package com.yilian.madp.admin.refund.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理员退款列表项。 */
public record AdminRefundListItemResponse(
        Long refundId,
        Long appointmentId,
        String orderNo,
        String patientName,
        String doctorName,
        BigDecimal amount,
        String reason,
        String note,
        String status,
        String appointmentStatus,
        Boolean canReject,
        String rejectBlockedReason,
        LocalDateTime createTime,
        LocalDateTime processTime
) {
}
