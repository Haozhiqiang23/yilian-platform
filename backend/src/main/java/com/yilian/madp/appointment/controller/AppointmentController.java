package com.yilian.madp.appointment.controller;

import com.yilian.madp.appointment.dto.CreateAppointmentRequest;
import com.yilian.madp.appointment.dto.ContactAdminRefundRequest;
import com.yilian.madp.appointment.dto.AppointmentListQuery;
import com.yilian.madp.appointment.service.AppointmentService;
import com.yilian.madp.appointment.vo.CreateAppointmentResponse;
import com.yilian.madp.appointment.vo.AppointmentListItemResponse;
import com.yilian.madp.appointment.vo.AppointmentDetailResponse;
import com.yilian.madp.appointment.vo.AppointmentStatusResponse;
import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.common.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者预约接口层。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;

    /**
     * 使用当前 JWT 用户创建预约，患者 ID 不允许由前端传入。
     */
    @PostMapping
    public Result<CreateAppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "预约创建成功",
                appointmentService.createAppointment(
                        currentUser.getUserId(),
                        request.getSourceId()
                )
        );
    }

    /**
     * 查询当前患者自己的预约列表。
     */
    @GetMapping("/my")
    public Result<PageResult<AppointmentListItemResponse>> getMyAppointments(
            @Valid @ModelAttribute AppointmentListQuery query
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                appointmentService.getMyAppointments(
                        currentUser.getUserId(),
                        query
                )
        );
    }

    /**
     * 查询当前患者自己的预约详情。
     */
    @GetMapping("/{appointmentId}")
    public Result<AppointmentDetailResponse> getAppointmentDetail(
            @PathVariable Long appointmentId
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                appointmentService.getAppointmentDetail(
                        currentUser.getUserId(),
                        appointmentId
                )
        );
    }

    /**
     * 取消当前患者自己的预约。
     */

    @PutMapping("/{appointmentId}/cancel")
    public Result<AppointmentStatusResponse> cancelAppointment(
            @PathVariable Long appointmentId
    ) {

        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        AppointmentStatusResponse response =
                appointmentService.cancelAppointment(
                        currentUser.getUserId(),
                        appointmentId
                );

        return Result.success(
                "退款中".equals(response.status())
                        ? "退款申请已提交"
                        : "预约已取消",
                response
        );

    }

    /** 超过自助取消时限后，患者可说明原因并申请管理员人工处理退款。 */
    @PostMapping("/{appointmentId}/contact-refund")
    public Result<AppointmentStatusResponse> contactAdminForRefund(
            @PathVariable Long appointmentId,
            @Valid @RequestBody ContactAdminRefundRequest request
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "退款申请已提交，请等待管理员处理",
                appointmentService.contactAdminForRefund(
                        currentUser.getUserId(), appointmentId, request.getReason()
                )
        );
    }
}
