package com.yilian.madp.appointment.vo;

import java.math.BigDecimal;

public record CancelPaymentLockResponse( Long paymentId,
                                         BigDecimal amount,
                                         String status) {

}
