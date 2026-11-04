package com.yilian.madp.admin.queue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 管理员异常调序请求。 */
public record AdjustQueueRequest(
        @NotNull(message = "目标位置不能为空")
        @Min(value = 1, message = "目标位置不能小于1")
        Integer position,

        @NotBlank(message = "调整原因不能为空")
        @Size(max = 100, message = "调整原因不能超过100个字符")
        String reason
) {
}
