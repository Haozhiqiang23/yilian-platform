package com.yilian.madp.admin.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员确认退款结果请求。 */
@Data
public class ProcessRefundRequest {

    @NotBlank(message = "退款状态不能为空")
    @Pattern(regexp = "退款成功|退款拒绝", message = "退款状态不合法")
    private String status;

    @Size(max = 255, message = "处理备注不能超过255个字符")
    private String note;
}
