package com.yilian.madp.queue.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.queue.service.QueueService;
import com.yilian.madp.queue.vo.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 患者排队状态接口层。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queues")
public class QueueController {

    private final QueueService queueService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{appointmentId}")
    public Result<QueueStatusResponse> getQueue(
            @PathVariable Long appointmentId
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                queueService.getPatientQueue(
                        currentUser.getUserId(),
                        appointmentId
                )
        );

    }
}
