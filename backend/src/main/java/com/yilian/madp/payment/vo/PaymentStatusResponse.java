package com.yilian.madp.payment.vo;

/**
 * 模拟支付完成后返回给前端的状态。
 */
public record PaymentStatusResponse(
        Long paymentId,
        String status
) {
}
