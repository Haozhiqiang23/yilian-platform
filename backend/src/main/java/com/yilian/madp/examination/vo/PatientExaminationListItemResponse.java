package com.yilian.madp.examination.vo;

import java.time.LocalDateTime;

/** 患者查看本人检查申请时使用的列表数据。 */
public record PatientExaminationListItemResponse(
        Long examinationId,
        Long medicalRecordId,
        String itemName,
        String status,
        Long reportId,
        String result,
        String remark,
        String doctorName,
        String department,
        LocalDateTime createTime
) {
}
