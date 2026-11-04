package com.yilian.madp.prescription.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 医生编辑处方草稿时提交的完整内容。 */
@Data
public class UpdatePrescriptionRequest {

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

    @NotBlank(message = "处方状态不能为空")
    @Pattern(regexp = "^(草稿|已提交)$", message = "处方状态只能是草稿或已提交")
    private String status;
}
