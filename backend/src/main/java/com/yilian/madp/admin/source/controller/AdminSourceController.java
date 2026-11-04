package com.yilian.madp.admin.source.controller;

import com.yilian.madp.admin.source.dto.AdminSourceListQuery;
import com.yilian.madp.admin.source.dto.UpdateAdminSourceRequest;
import com.yilian.madp.admin.source.dto.UpdateSourceStatusRequest;
import com.yilian.madp.admin.source.dto.CreateAdminSourceRequest;
import com.yilian.madp.admin.source.service.AdminSourceService;
import com.yilian.madp.admin.source.vo.AdminSourcePageResponse;
import com.yilian.madp.admin.source.vo.AdminSourceUpdateResponse;
import com.yilian.madp.admin.source.vo.SourceSuspensionPreviewResponse;
import com.yilian.madp.admin.source.vo.SourceStatusResponse;
import com.yilian.madp.admin.source.vo.AdminSourceCreateResponse;
import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员号源管理接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sources")
public class AdminSourceController {

    private final AdminSourceService adminSourceService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public Result<AdminSourceCreateResponse> createSource(
            @Valid @RequestBody CreateAdminSourceRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "新建号源成功",
                adminSourceService.createSource(adminId, request)
        );
    }

    @GetMapping
    public Result<AdminSourcePageResponse> getSources(
            @Valid @ModelAttribute AdminSourceListQuery query
    ) {
        return Result.success(
                "查询管理员号源列表成功",
                adminSourceService.getSources(query)
        );
    }

    @PutMapping("/{id}")
    public Result<AdminSourceUpdateResponse> updateSource(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminSourceRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "调整号源成功",
                adminSourceService.updateSource(adminId, id, request)
        );
    }

    @GetMapping("/{id}/suspension-preview")
    public Result<SourceSuspensionPreviewResponse> getSuspensionPreview(
            @PathVariable Long id
    ) {
        return Result.success(
                "查询停诊影响成功",
                adminSourceService.getSuspensionPreview(id)
        );
    }

    @PutMapping("/{id}/status")
    public Result<SourceStatusResponse> updateSourceStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSourceStatusRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "号源状态修改成功",
                adminSourceService.updateSourceStatus(adminId, id, request)
        );
    }
}
