package com.yilian.madp.medicalrecord.vo;

import java.time.LocalDateTime;

/** “我的诊疗记录”列表中的单条数据。 */
public record MedicalRecordListItemResponse(
        Long medicalRecordId,
        Long appointmentId,
        Long doctorId,
        String doctorName,
        String department,
        String diagnosis,
        String treatment,
        String status,
        LocalDateTime createTime,
        Long prescriptionId,
        Long examinationId
) {
}
