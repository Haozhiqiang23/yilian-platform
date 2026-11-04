package com.yilian.madp.examination.vo;

import java.time.LocalDateTime;

/** 检查报告详情，同时包含检查项目和接诊医生信息。 */
public record ExaminationReportResponse(
        Long id,
        Long examinationId,
        Long medicalRecordId,
        String itemName,
        String examName,
        String status,
        String result,
        String conclusion,
        String remark,
        LocalDateTime createTime,
        Long doctorId,
        String doctorName,
        String department
) {
}
