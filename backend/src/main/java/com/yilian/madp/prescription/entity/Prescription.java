package com.yilian.madp.prescription.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 处方实体，与 prescription 表对应。 */
@Data
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long medicalRecordId;

    private String status;

    private String medicineInfo;

    private String usageInfo;

    private BigDecimal amount;

    private LocalDateTime createTime;
}
