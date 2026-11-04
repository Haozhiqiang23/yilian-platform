package com.yilian.madp.signin.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.signin.dto.SignInRequest;
import com.yilian.madp.signin.service.SignInService;
import com.yilian.madp.signin.vo.SignInResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 患者签到接口层。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sign-ins")
public class SignInController {

    private final SignInService signInService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public Result<SignInResponse> signIn(
            @Valid @RequestBody SignInRequest request
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "签到成功",
                signInService.signIn(
                        currentUser.getUserId(),
                        request.getAppointmentId()
                )
        );
    }
}
