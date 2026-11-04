package com.yilian.madp.source.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 号源数据库实体，与 source 表字段对应。
 * 使用 AppointmentSource 命名，避免和 Java 中过于宽泛的 Source 概念混淆。
 */
@Data
@TableName("source")
public class AppointmentSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private LocalDate visitDate;

    /** 兼容旧数据的上午/下午标识；完成全链路切换后删除。 */
    private String timePeriod;

    /** 出诊开始时间。 */
    private LocalTime startTime;

    /** 出诊结束时间。 */
    private LocalTime endTime;

    private Integer totalCount;

    private Integer remainCount;

    /** 号源业务状态：正常、停诊。 */
    private String status;

    private String suspensionReason;

    private LocalDateTime suspensionTime;

    private Long suspendedBy;
}
