package com.yilian.madp.doctorqueue.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.doctorqueue.dto.DoctorQueueQuery;
import com.yilian.madp.doctorqueue.service.DoctorQueueService;
import com.yilian.madp.doctorqueue.vo.DoctorQueueItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 医生本人患者队列接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor/queues")
public class DoctorQueueController {

    private final DoctorQueueService doctorQueueService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<DoctorQueueItemResponse>> getMyQueue(
            @Valid @ModelAttribute DoctorQueueQuery query
    ) {
        return Result.success(
                "查询成功",
                doctorQueueService.getMyQueue(
                        currentUserService.getCurrentUser().getUserId(),
                        query
                )
        );
    }
}
