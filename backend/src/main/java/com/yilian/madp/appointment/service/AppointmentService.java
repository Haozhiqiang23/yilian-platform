package com.yilian.madp.appointment.service;

import com.yilian.madp.appointment.entity.Appointment;
import com.yilian.madp.appointment.dto.AppointmentListQuery;
import com.yilian.madp.appointment.mapper.AppointmentMapper;
import com.yilian.madp.appointment.vo.AppointmentDetailResponse;
import com.yilian.madp.appointment.vo.AppointmentListItemResponse;
import com.yilian.madp.appointment.vo.AppointmentStatusResponse;
import com.yilian.madp.appointment.vo.CancelPaymentLockResponse;
import com.yilian.madp.appointment.vo.CreateAppointmentResponse;
import com.yilian.madp.appointment.vo.SourceLockResponse;

import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import com.yilian.madp.message.service.MessageNotificationService;
import com.yilian.madp.refund.entity.Refund;
import com.yilian.madp.refund.mapper.RefundMapper;
import com.yilian.madp.systemconfig.service.RuntimeSystemConfigService;
import com.yilian.madp.payment.service.PaymentExpirationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 患者预约业务层，负责号源校验、并发控制和预约创建。
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String STATUS_PENDING_PAYMENT = "待支付";
    private static final String STATUS_PAID = "已支付";
    private static final String STATUS_CANCELLED = "取消";
    private static final String STATUS_REFUNDING = "退款中";
    private static final String PAYMENT_SUCCESS = "支付成功";

    private final AppointmentMapper appointmentMapper;
    private final PatientMapper patientMapper;
    private final RefundMapper refundMapper;
    private final MessageNotificationService messageNotificationService;
    private final RuntimeSystemConfigService runtimeSystemConfigService;
    private final PaymentExpirationService paymentExpirationService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 创建预约和扣减号源必须处于同一事务：
     * 任意一步失败时，数据库会同时回滚预约和号源变化。
     */
    public CreateAppointmentResponse createAppointment(
            Long userId,
            Long sourceId
    ) {
        // 提交新预约前先释放已超时的待支付预约，避免旧记录造成同一时段冲突误判。
        paymentExpirationService.expirePendingAppointments();

        // 【并发修复：事务边界】先完成清理并归还连接，再开始预约事务。
        // 不能在已有事务里等待REQUIRES_NEW申请第二条连接，否则并发时可能耗尽连接池。
        // TransactionTemplate保证下面的扣库存、插预约和通知仍然原子提交/回滚，
        // 也避免将@Transactional放在本类私有方法上导致自调用绕过Spring代理。
        return new TransactionTemplate(transactionManager).execute(
                status -> createAppointmentInTransaction(userId, sourceId)
        );
    }

    private CreateAppointmentResponse createAppointmentInTransaction(Long userId, Long sourceId) {

        Long patientId = appointmentMapper.selectPatientIdForUpdate(userId);
        if (patientId == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        SourceLockResponse source =
                appointmentMapper.selectSourceForUpdate(sourceId);
        if (source == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND);
        }

        if (!"正常".equals(source.status())) {
            throw new BusinessException(ErrorCode.SOURCE_SUSPENDED);
        }

        // 号源结束后不能再新建预约；就诊时段进行中仍允许预约。
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime visitEnd = source.visitDate().atTime(source.endTime());
        if (!now.isBefore(visitEnd)) {
            throw new BusinessException(ErrorCode.SOURCE_APPOINTMENT_EXPIRED);
        }

        if (source.remainCount() <= 0) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_ENOUGH);
        }

        if (appointmentMapper.countActiveSameTimeAppointment(
                patientId,
                sourceId
        ) > 0) {
            throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_EXISTS);
        }

        // 即使已经加锁，更新 SQL 仍检查 remain_count > 0，避免出现负库存。
        if (appointmentMapper.decreaseSource(sourceId) != 1) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_ENOUGH);
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(source.doctorId());
        appointment.setSourceId(sourceId);
        appointment.setStatus(STATUS_PENDING_PAYMENT);
        appointmentMapper.insert(appointment);

        // 与预约处于同一事务：消息写入失败时，预约和号源扣减也会一起回滚。
        messageNotificationService.createAppointmentSuccess(
                userId,
                appointment.getId()
        );

        return new CreateAppointmentResponse(
                appointment.getId(),
                appointment.getStatus()
        );
    }

    /**
     * 分页查询当前患者自己的预约，可按预约状态筛选。
     */
    public PageResult<AppointmentListItemResponse> getMyAppointments(
            Long userId,
            AppointmentListQuery query
    ) {
        paymentExpirationService.expirePendingAppointments();

        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        if (query.getStatus() != null) {
            query.setStatus(query.getStatus().trim());
        }

        int total = appointmentMapper.countMine(
                patient.getId(),
                query.getStatus()
        );
        int offset = (query.getPage() - 1) * query.getSize();

        List<AppointmentListItemResponse> records =
                appointmentMapper.selectMine(
                        patient.getId(),
                        query.getStatus(),
                        offset,
                        query.getSize()
                );

        return new PageResult<>(records, total);

    }

    /**
     * 查询当前患者自己的预约详情。
     */
    public AppointmentDetailResponse getAppointmentDetail(
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

        AppointmentDetailResponse detail =
                appointmentMapper.selectOwnedDetail(
                        patient.getId(),
                        appointmentId
                );
        if (detail == null) {
            // 他人的预约也统一返回不存在，避免泄露预约信息。
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        return detail;
    }

    /**
     * 取消当前患者自己的预约。
     *
     * 待支付预约直接取消并恢复号源；已支付预约生成退款申请。
     * 整个方法处于同一事务，任意数据库操作失败都会全部回滚。
     */
    @Transactional
    public AppointmentStatusResponse cancelAppointment(
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

        Appointment appointment =
                appointmentMapper.selectOwnedAppointmentForUpdate(
                        patient.getId(),
                        appointmentId
                );
        if (appointment == null) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
//检查取消时间
        checkCancellationDeadline(appointment);

        if (STATUS_PENDING_PAYMENT.equals(appointment.getStatus())) {
            cancelPendingAppointment(appointment);
            messageNotificationService.createAppointmentCancelled(
                    userId,
                    appointmentId,
                    false
            );
            return new AppointmentStatusResponse(
                    appointmentId,
                    STATUS_CANCELLED
            );
        }

        if (STATUS_PAID.equals(appointment.getStatus())) {
            // 系统开关只限制患者主动申请退款。
            // 待支付预约仍可直接取消，医生停诊的强制退款也不经过这个方法。
            runtimeSystemConfigService.checkRefundEnabled();
            createRefundRequest(appointment, "患者取消预约");
            messageNotificationService.createAppointmentCancelled(
                    userId,
                    appointmentId,
                    true
            );
            return new AppointmentStatusResponse(
                    appointmentId,
                    STATUS_REFUNDING
            );
        }

        throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
    }

    /**
     * 已支付预约无法自助取消时转交管理员人工审核。
     * 该入口不受自助取消时限限制，但仍要求预约归属正确且支付已成功。
     */
    @Transactional
    public AppointmentStatusResponse contactAdminForRefund(
            Long userId,
            Long appointmentId,
            String reason
    ) {
        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery().eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        Appointment appointment = appointmentMapper.selectOwnedAppointmentForUpdate(
                patient.getId(), appointmentId
        );
        if (appointment == null) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!STATUS_PAID.equals(appointment.getStatus())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        createRefundRequest(appointment, "患者联系管理员退款：" + reason.trim());
        messageNotificationService.createAppointmentCancelled(userId, appointmentId, true);
        return new AppointmentStatusResponse(appointmentId, STATUS_REFUNDING);
    }

    /**
     * 在待支付和已支付两种可取消状态中都执行时间校验，防止用户在就诊前最后时刻取消。
     */
    private void checkCancellationDeadline(Appointment appointment) {

        LocalDateTime visitStart = appointmentMapper.selectVisitStartTime(appointment.getSourceId());
        if (visitStart == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND);
        }
//截止日期
        LocalDateTime deadline = visitStart.minusHours(
                runtimeSystemConfigService.getAppointmentCancelHours()
        );
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(ErrorCode.APPOINTMENT_CANCEL_DEADLINE);
        }

    }

    /** 待支付预约取消后恢复号源，并关闭可能已经创建的支付单。 */
    private void cancelPendingAppointment(Appointment appointment) {
        if (appointmentMapper.cancelPendingAppointment(
                appointment.getId()
        ) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        if (appointmentMapper.increaseSource(
                appointment.getSourceId()
        ) != 1) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND);
        }

        // 支付单不一定已创建，影响 0 行属于正常情况。
        appointmentMapper.closePendingPayment(appointment.getId());
    }

    /** 已支付预约取消时创建退款申请，等待管理员后续处理。 */
    private void createRefundRequest(Appointment appointment, String reason) {
        CancelPaymentLockResponse payment =
                appointmentMapper.selectPaymentForUpdate(
                        appointment.getId()
                );
        if (payment == null || !PAYMENT_SUCCESS.equals(payment.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
        }

        Refund refund = new Refund();
        refund.setPaymentId(payment.paymentId());
        refund.setAmount(payment.amount());
        refund.setStatus("退款申请");
        refund.setReason(reason);
        refundMapper.insert(refund);

        if (appointmentMapper.markAppointmentRefunding(
                appointment.getId()
        ) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

    }
}
