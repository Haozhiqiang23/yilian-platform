package com.yilian.madp.payment.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.Result;
import com.yilian.madp.payment.dto.CreatePaymentRequest;
import com.yilian.madp.payment.service.PaymentService;
import com.yilian.madp.payment.vo.CreatePaymentResponse;
import com.yilian.madp.payment.vo.PaymentStatusResponse;
import com.yilian.madp.payment.vo.PaymentDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者支付接口层。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    /**
     * 当前用户身份从 JWT 获取，前端只需要提交预约 ID。
     */
    @PostMapping
    public Result<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "支付订单创建成功",
                paymentService.createPayment(
                        currentUser.getUserId(),
                        request.getAppointmentId()
                )
        );
    }

    /**
     * 模拟支付成功；重复调用已成功的订单仍返回成功。
     */
    @PostMapping("/{paymentId}/pay")
    public Result<PaymentStatusResponse> pay(
            @PathVariable Long paymentId
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "支付成功",
                paymentService.pay(
                        currentUser.getUserId(),
                        paymentId
                )
        );
    }

    /**
     * 查询当前患者自己的支付订单详情。
     */
    @GetMapping("/{paymentId}")
    public Result<PaymentDetailResponse> getPaymentDetail(
            @PathVariable Long paymentId
    ) {
        CurrentUserResponse currentUser =
                currentUserService.getCurrentUser();

        return Result.success(
                "查询成功",
                paymentService.getPaymentDetail(
                        currentUser.getUserId(),
                        paymentId
                )
        );
    }

    /** 查询预约已有的支付订单，重复进入支付页时直接复用。 */
    @GetMapping("/by-appointment/{appointmentId}")
    public Result<PaymentDetailResponse> getPaymentByAppointment(
            @PathVariable Long appointmentId
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser();
        return Result.success(
                "查询成功",
                paymentService.getPaymentByAppointment(
                        currentUser.getUserId(), appointmentId
                )
        );
    }

}
