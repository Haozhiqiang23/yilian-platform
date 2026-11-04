package com.yilian.madp.appointment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约数据库实体，与 appointment 表字段对应。
 */
@Data
@TableName("appointment")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long doctorId;

    private Long sourceId;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
