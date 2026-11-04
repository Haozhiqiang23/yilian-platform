package com.yilian.madp.prescription.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 患者或开方医生可查看的处方详情。 */
public record PrescriptionDetailResponse(
        Long prescriptionId,
        Long medicalRecordId,
        Long appointmentId,
        String status,
        String medicineInfo,
        String usageInfo,
        BigDecimal amount,
        LocalDateTime createTime,
        Long doctorId,
        String doctorName,
        String department
) {
}
