package com.yilian.madp.medicalrecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 诊疗记录实体，与 medical_record 表对应。 */
@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appointmentId;

    private Long doctorId;

    private Long patientId;

    private String diagnosis;

    private String treatment;

    private LocalDateTime createTime;
}
