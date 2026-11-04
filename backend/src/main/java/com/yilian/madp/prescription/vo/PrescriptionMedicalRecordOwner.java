package com.yilian.madp.prescription.vo;

/**
 * 创建处方前使用的诊疗记录归属信息。
 * 该对象只服务于后端业务校验，不直接返回给前端。
 */
public record PrescriptionMedicalRecordOwner(
        Long medicalRecordId,
        Long patientUserId
) {
}
