package com.yilian.madp.prescription.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 医生处方列表中的单条数据。 */
public record DoctorPrescriptionListItemResponse(
        Long prescriptionId,
        Long medicalRecordId,
        Long appointmentId,
        Long patientId,
        String patientName,
        String status,
        String medicineInfo,
        String usageInfo,
        BigDecimal amount,
        LocalDateTime createTime
) {
}
