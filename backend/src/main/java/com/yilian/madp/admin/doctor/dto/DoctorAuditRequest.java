package com.yilian.madp.admin.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

/** 管理员审核医生请求。 */
public class DoctorAuditRequest {

    @NotBlank(message = "审核结果不能为空")
    @Pattern(
            regexp = "PASS|APPROVE|REJECT",
            message = "审核结果只能是 PASS、APPROVE 或 REJECT"
    )
    private String result;

    @Size(max = 100, message = "审核原因不能超过100个字符")
    private String reason;

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    /**
     * 审核通过时原因可不填写；审核拒绝时必须说明原因，便于医生修改资料和管理员追溯。
     */
    @AssertTrue(message = "拒绝医生审核时必须填写原因")
    public boolean isRejectReasonValid() {
        return !"REJECT".equals(result) || (reason != null && !reason.trim().isEmpty());
    }
}
