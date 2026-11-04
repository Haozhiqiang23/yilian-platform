package com.yilian.madp.examination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 检查报告实体，与 examination_report 表对应。 */
@Data
@TableName("examination_report")
public class ExaminationReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examinationId;

    private String result;

    private String remark;

    private LocalDateTime createTime;
}
