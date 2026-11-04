package com.yilian.madp.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建支付订单后返回给前端的数据。
 */
public record CreatePaymentResponse(
        Long paymentId,
        String orderNo,
        BigDecimal amount,
        String status,
        LocalDateTime expiresAt
) {
}
