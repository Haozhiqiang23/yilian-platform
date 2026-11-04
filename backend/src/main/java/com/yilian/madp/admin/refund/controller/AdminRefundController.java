package com.yilian.madp.admin.refund.controller;

import com.yilian.madp.admin.refund.dto.AdminRefundListQuery;
import com.yilian.madp.admin.refund.dto.ProcessRefundRequest;
import com.yilian.madp.admin.refund.service.AdminRefundService;
import com.yilian.madp.admin.refund.vo.AdminRefundListItemResponse;
import com.yilian.madp.admin.refund.vo.RefundProcessResponse;
import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员退款管理接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/refunds")
public class AdminRefundController {

    private final AdminRefundService adminRefundService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<AdminRefundListItemResponse>> getRefunds(
            @Valid @ModelAttribute AdminRefundListQuery query
    ) {
        return Result.success(
                "查询退款列表成功",
                adminRefundService.getRefunds(query)
        );
    }

    @PutMapping("/{id}")
    public Result<RefundProcessResponse> processRefund(
            @PathVariable Long id,
            @Valid @RequestBody ProcessRefundRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "退款处理成功",
                adminRefundService.processRefund(adminId, id, request)
        );
    }
}
