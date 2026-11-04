package com.yilian.madp.examination.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.examination.dto.CreateExaminationRequest;
import com.yilian.madp.examination.dto.CompleteExaminationRequest;
import com.yilian.madp.examination.dto.SubmitExaminationResultRequest;
import com.yilian.madp.examination.service.ExaminationService;
import com.yilian.madp.examination.vo.CreateExaminationResponse;
import com.yilian.madp.examination.vo.ExaminationReportResponse;
import com.yilian.madp.examination.vo.PatientExaminationListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 检查申请和检查报告接口层。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/examinations")
public class ExaminationController {

    private final ExaminationService examinationService;
    private final CurrentUserService currentUserService;

    /** 创建检查申请；接口权限由 SecurityConfig 限制为医生角色。 */
    @PostMapping
    public Result<CreateExaminationResponse> create(
            @Valid @RequestBody CreateExaminationRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "检查申请创建成功",
                examinationService.create(currentUser.getUserId(), request)
        );
    }

    /** 患者查看本人全部检查申请及当前处理状态。 */
    @GetMapping("/my")
    public Result<List<PatientExaminationListItemResponse>> getMine() {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "查询成功",
                examinationService.getPatientExaminations(currentUser.getUserId())
        );
    }

    /** 患者提交线下检查结果，交由开单医生确认。 */
    @PutMapping("/{id}/patient-result")
    public Result<PatientExaminationListItemResponse> submitPatientResult(
            @PathVariable Long id,
            @Valid @RequestBody SubmitExaminationResultRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "检查结果已提交，请等待医生确认",
                examinationService.submitPatientResult(
                        currentUser.getUserId(), id, request
                )
        );
    }

    /** 录入检查结果并完成检查申请。 */
    @PutMapping("/{id}/result")
    public Result<ExaminationReportResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteExaminationRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "检查结果录入成功",
                examinationService.complete(
                        currentUser.getUserId(),
                        id,
                        request
                )
        );
    }

    /** 查询检查报告，患者和接诊医生均可查看归属于自己的数据。 */
    @GetMapping("/{id}/report")
    public Result<ExaminationReportResponse> getReport(
            @PathVariable Long id
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                examinationService.getReport(id, currentUser)
        );
    }
}
