package com.yilian.madp.auth.controller;

import com.yilian.madp.auth.dto.LoginRequest;
import com.yilian.madp.auth.dto.RegisterRequest;
import com.yilian.madp.auth.dto.VerifyCodeRequest;
import com.yilian.madp.auth.service.AuthService;
import com.yilian.madp.auth.service.VerifyCodeService;
import com.yilian.madp.auth.vo.LoginResponse;
import com.yilian.madp.auth.vo.RegisterResponse;
import com.yilian.madp.auth.vo.VerifyCodeResponse;
import com.yilian.madp.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    private final VerifyCodeService verifyCodeService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        log.info("用户尝试登录，账号：{}", request.getPhone());
        return Result.success("登录成功", response);
    }

    /**
     * 获取开发阶段注册验证码。
     * 当前实际验证码固定为 123456，正式环境再接短信服务。
     */
    @PostMapping("/verify-code")
    public Result<VerifyCodeResponse> getVerifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return Result.success(
                "验证码已生成，开发验证码为123456",
                verifyCodeService.create(request.getPhone())
        );
    }
    /**
     * 患者实名注册入口。
     */
    @PostMapping("/register")
    public Result<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return Result.success("注册成功", authService.register(request));
    }
}
