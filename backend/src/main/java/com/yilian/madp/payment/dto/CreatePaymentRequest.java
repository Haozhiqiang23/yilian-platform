package com.yilian.madp.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 创建支付订单请求。
 */
@Data
public class CreatePaymentRequest {

    @NotNull(message = "预约 ID 不能为空")
    @Positive(message = "预约 ID 必须大于 0")
    private Long appointmentId;
}
