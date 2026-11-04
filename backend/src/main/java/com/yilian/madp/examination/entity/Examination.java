package com.yilian.madp.examination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 检查申请实体，与 examination 表对应。 */
@Data
@TableName("examination")
public class Examination {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long medicalRecordId;

    private String itemName;

    private String status;

    private LocalDateTime createTime;
}
