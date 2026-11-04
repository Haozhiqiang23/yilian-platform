package com.yilian.madp.examination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 医生退回患者检查结果时填写的原因。 */
@Data
public class RejectExaminationRequest {

    @NotBlank(message = "退回原因不能为空")
    @Size(max = 500, message = "退回原因不能超过 500 个字符")
    private String reason;
}
