package com.yilian.madp.admin.source.vo;

import java.math.BigDecimal;

/** 停诊处理时锁定读取的有效预约及患者账号。 */
public record SourceAffectedAppointmentResponse(
        Long appointmentId,
        Long recipientUserId,
        String status,
        Long paymentId,
        BigDecimal paymentAmount,
        String paymentStatus
) {
}
