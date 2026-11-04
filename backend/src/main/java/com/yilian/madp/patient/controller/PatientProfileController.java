package com.yilian.madp.patient.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.patient.dto.UpdatePatientProfileRequest;
import com.yilian.madp.patient.service.PatientProfileService;
import com.yilian.madp.patient.vo.PatientProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 患者个人信息接口层，只负责接收请求并返回统一结果。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patients/profile")
public class PatientProfileController {

    private final CurrentUserService currentUserService;
    private final PatientProfileService patientProfileService;

    /**
     * 查询当前登录患者的个人信息。
     */
    @GetMapping
    public Result<PatientProfileResponse> getProfile() {
        // 用户 ID 来自已经通过校验的 JWT，不能由前端随意传入。
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        PatientProfileResponse profile = patientProfileService.getProfile(
                currentUser.getUserId()
        );

        return Result.success("查询患者个人信息成功", profile);
    }

    /**
     * 修改当前患者的手机号和联系地址。
     */
    @PutMapping
    public Result<PatientProfileResponse> updateProfile(
            @Valid @RequestBody UpdatePatientProfileRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        PatientProfileResponse profile = patientProfileService.updateProfile(
                currentUser.getUserId(),
                request
        );

        return Result.success("患者个人信息修改成功", profile);
    }
}
