package com.yilian.madp.examination.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.examination.dto.DoctorExaminationQuery;
import com.yilian.madp.examination.dto.RejectExaminationRequest;
import com.yilian.madp.examination.service.ExaminationService;
import com.yilian.madp.examination.vo.DoctorExaminationListItemResponse;
import com.yilian.madp.examination.vo.ExaminationReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 医生查看本人检查申请列表的接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor/examinations")
public class DoctorExaminationController {

    private final ExaminationService examinationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<DoctorExaminationListItemResponse>> getMine(
            @Valid @ModelAttribute DoctorExaminationQuery query
    ) {
        return Result.success(
                "查询成功",
                examinationService.getDoctorExaminations(
                        currentUserService.getCurrentUser().getUserId(),
                        query
                )
        );
    }

    @PutMapping("/{id}/confirm")
    public Result<ExaminationReportResponse> confirm(
            @PathVariable Long id
    ) {
        return Result.success(
                "检查报告已确认",
                examinationService.confirmPatientResult(
                        currentUserService.getCurrentUser().getUserId(), id
                )
        );
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectExaminationRequest request
    ) {
        examinationService.rejectPatientResult(
                currentUserService.getCurrentUser().getUserId(), id, request
        );
        return Result.success("已退回患者补充", null);
    }
}
