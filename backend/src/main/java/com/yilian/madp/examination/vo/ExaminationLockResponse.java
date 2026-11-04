package com.yilian.madp.examination.vo;

/** 录入检查结果前加锁读取的归属和状态信息。 */
public record ExaminationLockResponse(
        Long examinationId,
        String status,
        Long patientUserId
) {
}
