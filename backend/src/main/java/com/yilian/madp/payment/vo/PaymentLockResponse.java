package com.yilian.madp.payment.vo;

/**
 * 模拟支付时锁定支付订单所需的最小数据。
 */
public record PaymentLockResponse(
        Long paymentId,
        Long appointmentId,
        String status
) {
}
