package com.yilian.madp.admin.user.controller;

import com.yilian.madp.admin.user.dto.AdminUserListQuery;
import com.yilian.madp.admin.user.dto.UpdateUserStatusRequest;
import com.yilian.madp.admin.user.service.AdminUserService;
import com.yilian.madp.admin.user.vo.AdminUserListItemResponse;
import com.yilian.madp.admin.user.vo.AdminUserStatusResponse;
import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员用户管理接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<AdminUserListItemResponse>> getUsers(
            @Valid @ModelAttribute AdminUserListQuery query
    ) {
        return Result.success("查询用户列表成功", adminUserService.getUsers(query));
    }

    @PutMapping("/{id}/status")
    public Result<AdminUserStatusResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        Long currentAdminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "用户状态修改成功",
                adminUserService.updateStatus(currentAdminId, id, request)
        );
    }
}
