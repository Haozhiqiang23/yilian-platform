package com.yilian.madp.doctor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医生数据库实体，与 doctor 表字段对应。
 */
@Data
@TableName("doctor")
public class Doctor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long departmentId;

    private String name;

    private String title;

    private String introduction;

    private String status;

    private String disableReason;

    private LocalDateTime disableTime;

    private Long disabledBy;
}
