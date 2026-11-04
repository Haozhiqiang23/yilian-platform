package com.yilian.madp.admin.log.vo;

import java.time.LocalDateTime;

/** 管理员操作日志列表项；日志只读，不提供修改或删除接口。 */
public record AdminLogListItemResponse(
        Long id,
        Long operatorId,
        String operatorName,
        String operatorRole,
        String operationType,
        String action,
        String targetType,
        Long targetId,
        String target,
        String reason,
        String result,
        LocalDateTime createTime
) {
}
