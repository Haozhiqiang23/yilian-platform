package com.yilian.madp.admin.source.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员停诊或恢复号源请求。 */
public class UpdateSourceStatusRequest {

    @NotBlank(message = "号源状态不能为空")
    @Pattern(regexp = "SUSPENDED|NORMAL", message = "号源状态只能是SUSPENDED或NORMAL")
    private String status;

    @Size(max = 100, message = "停诊原因不能超过100个字符")
    private String reason;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
