package com.yilian.madp.user.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//没看
/**
 * 提供当前登录用户相关接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    /**
     * 验证 JWT 是否有效，并返回 Token 中的用户 ID 和角色。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> getCurrentUser() {
        return Result.success(
                "查询当前用户成功",
                currentUserService.getCurrentUser()
        );
    }
}
