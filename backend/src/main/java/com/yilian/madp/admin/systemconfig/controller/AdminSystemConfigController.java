package com.yilian.madp.admin.systemconfig.controller;

import com.yilian.madp.admin.systemconfig.dto.UpdateSystemConfigRequest;
import com.yilian.madp.admin.systemconfig.service.AdminSystemConfigService;
import com.yilian.madp.admin.systemconfig.vo.SystemConfigResponse;
import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员系统配置读取与保存接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/config")
public class AdminSystemConfigController {

    private final AdminSystemConfigService adminSystemConfigService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<SystemConfigResponse> getConfig() {
        return Result.success("查询系统配置成功", adminSystemConfigService.getConfig());
    }

    @PutMapping
    public Result<SystemConfigResponse> updateConfig(
            @Valid @RequestBody UpdateSystemConfigRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "保存系统配置成功",
                adminSystemConfigService.updateConfig(adminId, request)
        );
    }
}
