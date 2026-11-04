package com.yilian.madp.signin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 患者签到请求。 */
@Data
public class SignInRequest {

    @NotNull(message = "预约 ID 不能为空")
    @Positive(message = "预约 ID 必须大于 0")
    private Long appointmentId;
}
