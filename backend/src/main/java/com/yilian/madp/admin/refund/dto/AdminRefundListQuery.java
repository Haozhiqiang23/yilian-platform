package com.yilian.madp.admin.refund.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 管理员退款列表查询参数。 */
@Data
public class AdminRefundListQuery {

    private String status;

    private String keyword;

    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;
}
