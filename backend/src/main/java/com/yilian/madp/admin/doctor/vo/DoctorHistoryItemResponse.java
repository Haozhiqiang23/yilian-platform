package com.yilian.madp.admin.doctor.vo;

import java.time.LocalDateTime;

/** 医生审核和状态变更历史。 */
public record DoctorHistoryItemResponse(
        Long id,
        String operationType,
        String action,
        String target,
        String reason,
        String result,
        Long operatorId,
        String operatorName,
        LocalDateTime createTime
) {
}
