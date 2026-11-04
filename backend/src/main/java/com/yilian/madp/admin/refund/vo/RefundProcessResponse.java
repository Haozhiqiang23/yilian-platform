package com.yilian.madp.admin.refund.vo;

/** 管理员退款处理结果。 */
public record RefundProcessResponse(
        Long refundId,
        Long appointmentId,
        String status
) {
}
