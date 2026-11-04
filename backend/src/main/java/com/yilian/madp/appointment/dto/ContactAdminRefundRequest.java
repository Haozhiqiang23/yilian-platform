package com.yilian.madp.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 患者无法自助取消时，提交给管理员人工审核的退款说明。 */
@Data
public class ContactAdminRefundRequest {

    @NotBlank(message = "请填写退款原因")
    @Size(min = 5, max = 100, message = "退款原因请输入 5 至 100 个字符")
    private String reason;
}
