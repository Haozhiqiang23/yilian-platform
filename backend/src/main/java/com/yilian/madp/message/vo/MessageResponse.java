package com.yilian.madp.message.vo;

import java.time.LocalDateTime;

/** 患者站内消息返回数据。 */
public record MessageResponse(
        Long id,
        String title,
        String content,
        String type,
        Boolean read,
        LocalDateTime createdAt,
        LocalDateTime readTime,
        String relatedType,
        Long relatedId
) {
}
