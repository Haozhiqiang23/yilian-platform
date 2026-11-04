package com.yilian.madp.admin.doctor.controller;

import com.yilian.madp.admin.doctor.dto.AdminDoctorListQuery;
import com.yilian.madp.admin.doctor.dto.DoctorAuditListQuery;
import com.yilian.madp.admin.doctor.dto.DoctorAuditRequest;
import com.yilian.madp.admin.doctor.dto.DoctorHistoryQuery;
import com.yilian.madp.admin.doctor.dto.UpdateDoctorStatusRequest;
import com.yilian.madp.admin.doctor.service.AdminDoctorService;
import com.yilian.madp.admin.doctor.vo.AdminDoctorListItemResponse;
import com.yilian.madp.admin.doctor.vo.AdminDoctorDetailResponse;
import com.yilian.madp.admin.doctor.vo.DoctorAuditResponse;
import com.yilian.madp.admin.doctor.vo.DoctorStatusResponse;
import com.yilian.madp.admin.doctor.vo.DoctorHistoryItemResponse;
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

/** 管理员医生列表和待审核列表接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/doctors")
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<AdminDoctorListItemResponse>> getDoctors(
            @Valid @ModelAttribute AdminDoctorListQuery query
    ) {
        return Result.success("查询医生列表成功", adminDoctorService.getDoctors(query));
    }

    @GetMapping("/{id}")
    public Result<AdminDoctorDetailResponse> getDoctorDetail(
            @PathVariable Long id
    ) {
        return Result.success(
                "查询医生详情成功",
                adminDoctorService.getDoctorDetail(id)
        );
    }

    @GetMapping("/{id}/history")
    public Result<PageResult<DoctorHistoryItemResponse>> getDoctorHistory(
            @PathVariable Long id,
            @Valid @ModelAttribute DoctorHistoryQuery query
    ) {
        return Result.success(
                "查询医生操作历史成功",
                adminDoctorService.getDoctorHistory(id, query)
        );
    }

    @GetMapping("/audit")
    public Result<PageResult<AdminDoctorListItemResponse>> getPendingAudits(
            @Valid @ModelAttribute DoctorAuditListQuery query
    ) {
        return Result.success("查询待审核医生成功", adminDoctorService.getPendingAudits(query));
    }

    @PutMapping("/{id}/audit")
    public Result<DoctorAuditResponse> auditDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorAuditRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "医生审核完成",
                adminDoctorService.auditDoctor(adminId, id, request)
        );
    }

    @PutMapping("/{id}/status")
    public Result<DoctorStatusResponse> updateDoctorStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDoctorStatusRequest request
    ) {
        Long adminId = currentUserService.getCurrentUser().getUserId();
        return Result.success(
                "医生状态修改成功",
                adminDoctorService.updateDoctorStatus(adminId, id, request)
        );
    }
}
