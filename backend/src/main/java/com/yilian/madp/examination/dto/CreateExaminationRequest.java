package com.yilian.madp.examination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 医生创建检查申请时提交的参数。 */
@Data
public class CreateExaminationRequest {

    @NotNull(message = "诊疗记录 ID 不能为空")
    @Positive(message = "诊疗记录 ID 必须大于 0")
    private Long medicalRecordId;

    @NotBlank(message = "检查项目不能为空")
    @Size(max = 100, message = "检查项目不能超过 100 个字符")
    private String itemName;
}
