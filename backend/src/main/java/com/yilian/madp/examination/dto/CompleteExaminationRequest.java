package com.yilian.madp.examination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 医生录入检查结果时提交的参数。 */
@Data
public class CompleteExaminationRequest {

    @NotBlank(message = "检查结果不能为空")
    @Size(max = 5000, message = "检查结果不能超过 5000 个字符")
    private String result;

    @Size(max = 2000, message = "检查备注不能超过 2000 个字符")
    private String remark;
}
