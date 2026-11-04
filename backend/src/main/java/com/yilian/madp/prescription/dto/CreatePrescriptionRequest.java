package com.yilian.madp.prescription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 医生创建处方时提交的参数。 */
@Data
public class CreatePrescriptionRequest {

    @NotNull(message = "诊疗记录 ID 不能为空")
    @Positive(message = "诊疗记录 ID 必须大于 0")
    private Long medicalRecordId;

    @NotBlank(message = "药品信息不能为空")
    @Size(max = 2000, message = "药品信息不能超过 2000 个字符")
    private String medicineInfo;

    @NotBlank(message = "用法用量不能为空")
    @Size(max = 2000, message = "用法用量不能超过 2000 个字符")
    private String usageInfo;

    @NotNull(message = "处方金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "处方金额不能小于 0")
    @Digits(integer = 8, fraction = 2, message = "处方金额最多保留两位小数")
    private BigDecimal amount;
}
