package com.yilian.madp.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单详情，供前端支付页面展示。
 */
public record PaymentDetailResponse(
        Long paymentId,
        Long appointmentId,
        String orderNo,
        BigDecimal amount,
        String status,
        LocalDateTime payTime,
        LocalDateTime expiresAt
) {
}
