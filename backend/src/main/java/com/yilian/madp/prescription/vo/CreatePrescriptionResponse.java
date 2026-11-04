package com.yilian.madp.prescription.vo;

import java.math.BigDecimal;

/** 创建处方后返回给前端的关键数据。 */
public record CreatePrescriptionResponse(
        Long prescriptionId,
        Long medicalRecordId,
        String status,
        BigDecimal amount
) {
}
