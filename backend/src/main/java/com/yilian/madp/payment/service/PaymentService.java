package com.yilian.madp.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import com.yilian.madp.message.service.MessageNotificationService;
import com.yilian.madp.payment.entity.Payment;
import com.yilian.madp.payment.mapper.PaymentMapper;
import com.yilian.madp.payment.vo.AppointmentPaymentLockResponse;
import com.yilian.madp.payment.vo.CreatePaymentResponse;
import com.yilian.madp.payment.vo.PaymentLockResponse;
import com.yilian.madp.payment.vo.PaymentDetailResponse;
import com.yilian.madp.payment.vo.PaymentStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 支付业务层，本阶段只负责创建支付订单。
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    private static final String APPOINTMENT_PENDING_PAYMENT = "待支付";
    private static final String PAYMENT_PENDING = "待支付";
    private static final String PAYMENT_SUCCESS = "支付成功";

    private final PaymentMapper paymentMapper;
    private final PaymentExpirationService paymentExpirationService;
    private final PatientMapper patientMapper;
    private final MessageNotificationService messageNotificationService;

    /**
     * 为当前患者的待支付预约创建唯一支付订单。
     *
     * 预约行锁负责让同一预约的并发请求串行执行；数据库中的
     * appointment_id 唯一索引则作为防止重复订单的最后一道保护。
     */
    @Transactional
    public CreatePaymentResponse createPayment(
            Long userId,
            Long appointmentId
    ) {
        paymentExpirationService.expireAppointment(appointmentId);

        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        AppointmentPaymentLockResponse appointment =
                paymentMapper.selectOwnedAppointmentForUpdate(
                        patient.getId(),
                        appointmentId
                );
        if (appointment == null) {
            // 不区分“预约不存在”和“属于其他患者”，避免泄露他人预约信息。
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        if (!APPOINTMENT_PENDING_PAYMENT.equals(appointment.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_REQUIRED);
        }

        Long existingCount = paymentMapper.selectCount(
                Wrappers.<Payment>lambdaQuery()
                        .eq(Payment::getAppointmentId, appointmentId)
        );

        if (existingCount > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_EXIST);
        }

        Payment payment = new Payment();
        payment.setAppointmentId(appointmentId);
        payment.setOrderNo(createOrderNo());
        payment.setAmount(appointment.fee());
        payment.setStatus(PAYMENT_PENDING);

        try {
            paymentMapper.insert(payment);
        } catch (DuplicateKeyException exception) {
            // 唯一索引冲突统一转成明确的业务错误，而不是返回 500。
            throw new BusinessException(ErrorCode.PAYMENT_EXIST);
        }

        return new CreatePaymentResponse(
                payment.getId(),
                payment.getOrderNo(),
                payment.getAmount(),
                payment.getStatus(),
                appointment.createTime().plusMinutes(PAYMENT_TIMEOUT_MINUTES)
        );
    }

    /**
     * 模拟完成支付，并同步更新预约状态。
     *
     * 已成功的订单再次请求时直接返回成功，不重复更新数据库。
     * 这就是接口的幂等性：相同请求执行多次，最终结果保持一致。
     */
    @Transactional
    public PaymentStatusResponse pay(Long userId, Long paymentId) {
        paymentExpirationService.expirePayment(paymentId);

        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        // 【支付取消并发修复】与取消路径统一：先预约、后支付单。
        // 第一次只取关联ID，不信任它代表归属；随后按当前患者锁预约。
        // 最后的支付查询仍重新校验归属和读取最新支付状态，不使用快照做状态决策。
        Long appointmentId = paymentMapper.selectAppointmentIdByPaymentId(paymentId);
        if (appointmentId == null || paymentMapper.lockOwnedAppointmentId(
                patient.getId(), appointmentId) == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        PaymentLockResponse payment =
                paymentMapper.selectOwnedPaymentForUpdate(
                        patient.getId(),
                        paymentId
                );
        if (payment == null) {
            // 他人的支付单也统一返回不存在，避免泄露订单信息。
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        if (PAYMENT_SUCCESS.equals(payment.status())) {
            return new PaymentStatusResponse(paymentId, PAYMENT_SUCCESS);
        }

        if (!PAYMENT_PENDING.equals(payment.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
        }

        if (paymentMapper.markPaymentSuccess(paymentId) != 1) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
        }

        if (paymentMapper.markAppointmentPaid(payment.appointmentId()) != 1) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
        }

        // 只有首次支付成功会走到这里；重复支付在上方直接返回，不会重复发消息。
        // 消息与支付单、预约状态共用当前事务，任一步失败都会整体回滚。
        messageNotificationService.createPaymentSuccess(
                userId,
                paymentId,
                payment.appointmentId()
        );

        return new PaymentStatusResponse(paymentId, PAYMENT_SUCCESS);
    }

    /**
     * 查询支付详情。这里只有普通读取，不修改数据，因此不需要事务和行锁。
     */
    public PaymentDetailResponse getPaymentDetail(
            Long userId,
            Long paymentId
    ) {
        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        paymentExpirationService.expirePayment(paymentId);

        PaymentDetailResponse detail =
                paymentMapper.selectOwnedPaymentDetail(
                        patient.getId(),
                        paymentId
                );
        if (detail == null) {
            // 不区分订单不存在和属于其他患者，避免泄露订单信息。
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        return detail;
    }

    /** 查询当前患者指定预约已有的支付订单，避免重复创建订单。 */
    public PaymentDetailResponse getPaymentByAppointment(
            Long userId,
            Long appointmentId
    ) {
        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        paymentExpirationService.expireAppointment(appointmentId);

        PaymentDetailResponse detail = paymentMapper.selectOwnedPaymentByAppointment(
                patient.getId(), appointmentId
        );
        if (detail == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return detail;
    }

    /**
     * 时间戳便于排查订单生成时间，随机片段用于降低并发重号概率。
     */
    private String createOrderNo() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "PAY" + System.currentTimeMillis() + randomPart;
    }
}
