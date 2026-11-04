package com.yilian.madp.prescription.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.prescription.dto.DoctorPrescriptionQuery;
import com.yilian.madp.prescription.service.PrescriptionService;
import com.yilian.madp.prescription.vo.DoctorPrescriptionListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 医生查询本人处方列表的接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor/prescriptions")
public class DoctorPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<DoctorPrescriptionListItemResponse>> getMine(
            @Valid @ModelAttribute DoctorPrescriptionQuery query
    ) {
        return Result.success(
                "查询成功",
                prescriptionService.getDoctorPrescriptions(
                        currentUserService.getCurrentUser().getUserId(),
                        query
                )
        );
    }
}
