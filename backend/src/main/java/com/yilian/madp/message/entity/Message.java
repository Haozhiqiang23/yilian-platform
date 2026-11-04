package com.yilian.madp.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息实体，与 message 表对应。 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipientUserId;

    private String messageType;

    private String title;

    private String content;

    private String relatedType;

    private Long relatedId;

    private Boolean isRead;

    private LocalDateTime createTime;

    private LocalDateTime readTime;
}
