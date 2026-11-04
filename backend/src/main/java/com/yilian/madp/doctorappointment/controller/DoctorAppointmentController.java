package com.yilian.madp.doctorappointment.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.Result;
import com.yilian.madp.doctorappointment.service.DoctorAppointmentService;
import com.yilian.madp.doctorappointment.vo.DoctorAppointmentStatusResponse;
import com.yilian.madp.doctorappointment.dto.FinishAppointmentRequest;
import com.yilian.madp.doctorappointment.vo.FinishAppointmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 医生接诊操作接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor/appointments")
public class DoctorAppointmentController {

    private final DoctorAppointmentService doctorAppointmentService;
    private final CurrentUserService currentUserService;

    @PutMapping("/{id}/start")
    public Result<DoctorAppointmentStatusResponse> start(
            @PathVariable Long id
    ) {
        return Result.success(
                "开始接诊成功",
                doctorAppointmentService.start(
                        currentUserService.getCurrentUser().getUserId(),
                        id
                )
        );
    }

    @PutMapping("/{id}/finish")
    public Result<FinishAppointmentResponse> finish(
            @PathVariable Long id,
            @Valid @RequestBody FinishAppointmentRequest request
    ) {
        return Result.success(
                "完成接诊成功",
                doctorAppointmentService.finish(
                        currentUserService.getCurrentUser().getUserId(),
                        id,
                        request
                )
        );
    }
}
