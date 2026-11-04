package com.yilian.madp.medicalrecord.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.medicalrecord.dto.DoctorMedicalRecordQuery;
import com.yilian.madp.medicalrecord.service.DoctorMedicalRecordService;
import com.yilian.madp.medicalrecord.vo.MedicalRecordListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 医生查询患者历史诊疗记录接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor/medical-records")
public class DoctorMedicalRecordController {

    private final DoctorMedicalRecordService doctorMedicalRecordService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{patientId}")
    public Result<PageResult<MedicalRecordListItemResponse>> getPatientHistory(
            @PathVariable Long patientId,
            @Valid @ModelAttribute DoctorMedicalRecordQuery query
    ) {
        return Result.success(
                "查询成功",
                doctorMedicalRecordService.getPatientHistory(
                        currentUserService.getCurrentUser().getUserId(),
                        patientId,
                        query
                )
        );
    }
}
