package com.yilian.madp.admin.queue.controller;

import com.yilian.madp.admin.queue.dto.AdminQueueListQuery;
import com.yilian.madp.admin.queue.dto.AdjustQueueRequest;
import com.yilian.madp.admin.queue.service.AdminQueueService;
import com.yilian.madp.admin.queue.vo.AdminQueueListItemResponse;
import com.yilian.madp.admin.queue.vo.QueueAdjustResponse;
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

/** 管理员队列查询接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/queues")
public class AdminQueueController {

    private final AdminQueueService adminQueueService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<AdminQueueListItemResponse>> getQueues(
            @Valid @ModelAttribute AdminQueueListQuery query
    ) {
        return Result.success(
                "查询管理员队列成功",
                adminQueueService.getQueues(query)
        );
    }

    /** 管理员处理现场异常队列，原因会写入审计日志。 */
    @PutMapping("/{queueId}")
    public Result<QueueAdjustResponse> adjustQueue(
            @PathVariable Long queueId,
            @Valid @RequestBody AdjustQueueRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "队列顺序调整成功",
                adminQueueService.adjustQueue(adminId, queueId, request)
        );
    }
}
