package com.yilian.madp.examination.vo;

/** 创建检查申请后返回给前端的数据。 */
public record CreateExaminationResponse(
        Long examinationId,
        Long medicalRecordId,
        String itemName,
        String status
) {
}
