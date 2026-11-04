package com.yilian.madp.examination.vo;

/** 创建检查申请前使用的诊疗记录归属信息。 */
public record ExaminationMedicalRecordOwner(
        Long medicalRecordId,
        Long patientUserId
) {
}
