package com.yilian.madp.admin.refund.vo;

/** 退款处理事务中锁定读取的关联数据。 */
public record RefundProcessLockResponse(
        Long refundId,
        Long appointmentId,
        Long sourceId,
        Long recipientUserId,
        String status,
        String appointmentStatus,
        String reason
) {
}
