package com.yilian.madp.admin.doctor.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员启用或禁用医生账号的请求参数。 */
public class UpdateDoctorStatusRequest {

    @NotBlank(message = "医生状态不能为空")
    @Pattern(regexp = "NORMAL|DISABLED", message = "医生状态只能是 NORMAL 或 DISABLED")
    private String status;

    @Size(max = 100, message = "操作原因不能超过100个字符")
    private String reason;

    /** 禁用属于高风险操作，必须留下原因；恢复账号时原因可选。 */
    @AssertTrue(message = "禁用医生账号时必须填写原因")
    public boolean isDisableReasonValid() {
        return !"DISABLED".equals(status)
                || (reason != null && !reason.trim().isEmpty());
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
