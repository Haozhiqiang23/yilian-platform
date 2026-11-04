package com.yilian.madp.queue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 排队记录实体；名称避开 Java 自带的 Queue 接口。 */
@Data
@TableName("queue")
public class QueueRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appointmentId;

    private String queueNo;

    private LocalDateTime signTime;

    /** 业务排序值；管理员调序只修改该字段，不修改真实签到时间。 */
    private Integer sortNo;

    private String status;
}
