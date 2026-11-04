package com.yilian.madp.medicalrecord.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.medicalrecord.dto.MedicalRecordListQuery;
import com.yilian.madp.medicalrecord.service.MedicalRecordService;
import com.yilian.madp.medicalrecord.vo.MedicalRecordListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 患者诊疗记录接口层。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final CurrentUserService currentUserService;

    @GetMapping("/my")
    public Result<PageResult<MedicalRecordListItemResponse>> getMyRecords(
            @Valid @ModelAttribute MedicalRecordListQuery query
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                medicalRecordService.getMyRecords(
                        currentUser.getUserId(),
                        query
                )
        );
    }
}
