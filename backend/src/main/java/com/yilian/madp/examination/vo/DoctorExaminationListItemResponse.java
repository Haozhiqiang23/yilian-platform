package com.yilian.madp.examination.vo;

import java.time.LocalDateTime;

/** 医生检查申请列表中的单条数据。 */
public record DoctorExaminationListItemResponse(
        Long examinationId,
        Long medicalRecordId,
        Long appointmentId,
        Long patientId,
        String patientName,
        String itemName,
        String status,
        Long reportId,
        String result,
        String remark,
        LocalDateTime createTime
) {
}
