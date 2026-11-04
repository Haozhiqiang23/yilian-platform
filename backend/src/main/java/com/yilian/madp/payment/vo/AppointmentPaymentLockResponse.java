package com.yilian.madp.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建支付单时锁定预约所需的最小数据。
 */
public record AppointmentPaymentLockResponse(
        Long id,
        String status,
        BigDecimal fee,
        LocalDateTime createTime
) {
}
