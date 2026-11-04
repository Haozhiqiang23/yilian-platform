package com.yilian.madp.doctor.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.Result;
import com.yilian.madp.doctor.service.DoctorService;
import com.yilian.madp.doctor.vo.DoctorProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录医生的个人资料接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor/profile")
public class DoctorProfileController {

    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<DoctorProfileResponse> getProfile() {
        return Result.success(
                "查询医生个人资料成功",
                doctorService.getProfile(
                        currentUserService.getCurrentUser().getUserId()
                )
        );
    }
}
