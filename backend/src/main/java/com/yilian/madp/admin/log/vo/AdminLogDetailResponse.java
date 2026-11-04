package com.yilian.madp.admin.log.vo;

import java.time.LocalDateTime;

/**
 * 管理员操作日志详情。
 *
 * <p>日志表只保存稳定的目标类型和目标 ID；详情查询再关联业务表，
 * 补充医生、患者、预约等易读信息。这样既方便管理员阅读，也保留了排查问题所需的原始 ID。</p>
 */
public record AdminLogDetailResponse(
        Long id,
        Long operatorId,
        String operatorName,
        String operatorRole,
        String operationType,
        String action,
        String targetType,
        Long targetId,
        String target,
        String targetName,
        Long appointmentId,
        String patientName,
        String doctorName,
        String businessInfo,
        String reason,
        String result,
        LocalDateTime createTime
) {
}
