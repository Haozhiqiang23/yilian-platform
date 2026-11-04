package com.yilian.madp.prescription.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.prescription.dto.CreatePrescriptionRequest;
import com.yilian.madp.prescription.dto.UpdatePrescriptionRequest;
import com.yilian.madp.prescription.service.PrescriptionService;
import com.yilian.madp.prescription.vo.CreatePrescriptionResponse;
import com.yilian.madp.prescription.vo.PrescriptionDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 处方接口层。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final CurrentUserService currentUserService;

    /** 创建处方；接口权限由 SecurityConfig 限制为医生角色。 */
    @PostMapping
    public Result<CreatePrescriptionResponse> create(
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "处方创建成功",
                prescriptionService.create(currentUser.getUserId(), request)
        );
    }

    /** 编辑本人处方草稿，也可在本次操作中提交处方。 */
    @PutMapping("/{id}")
    public Result<PrescriptionDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePrescriptionRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "处方更新成功",
                prescriptionService.update(
                        currentUser.getUserId(),
                        id,
                        request
                )
        );
    }

    /** 查询处方详情，患者和开方医生均可访问自己的数据。 */
    @GetMapping("/{id}")
    public Result<PrescriptionDetailResponse> getDetail(
            @PathVariable Long id
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                prescriptionService.getDetail(id, currentUser)
        );
    }
}
