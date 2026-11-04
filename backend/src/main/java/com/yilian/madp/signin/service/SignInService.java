package com.yilian.madp.signin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import com.yilian.madp.message.service.MessageNotificationService;
import com.yilian.madp.queue.entity.QueueRecord;
import com.yilian.madp.queue.mapper.QueueMapper;
import com.yilian.madp.signin.entity.SignIn;
import com.yilian.madp.signin.mapper.SignInMapper;
import com.yilian.madp.signin.vo.SignInAppointmentLockResponse;
import com.yilian.madp.signin.vo.SignInResponse;
import com.yilian.madp.systemconfig.service.RuntimeSystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者签到业务层，负责签到校验、排队号生成和预约状态联动。
 */
@Service
@RequiredArgsConstructor
public class SignInService {

    private static final String APPOINTMENT_PAID = "已支付";
    private static final String QUEUE_WAITING = "等待接诊";

    private final SignInMapper signInMapper;
    private final QueueMapper queueMapper;
    private final PatientMapper patientMapper;
    private final MessageNotificationService messageNotificationService;
    private final RuntimeSystemConfigService runtimeSystemConfigService;

    /**
     * 签到、创建队列和修改预约状态必须全部成功，否则整个事务回滚。
     */
    @Transactional
    public SignInResponse signIn(Long userId, Long appointmentId) {
        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        SignInAppointmentLockResponse appointment =
                signInMapper.selectOwnedAppointmentForUpdate(
                        patient.getId(),
                        appointmentId
                );
        if (appointment == null) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        // 先检查签到记录，已签到的请求返回明确的重复签到错误。
        if (signInMapper.countByAppointmentId(appointmentId) > 0) {
            throw new BusinessException(ErrorCode.SIGN_ALREADY_EXISTS);
        }

        if (!APPOINTMENT_PAID.equals(appointment.status())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_PAY);
        }

        LocalDate today = LocalDate.now();
        if (!today.equals(appointment.visitDate())) {
            throw new BusinessException(ErrorCode.SIGN_TIME_ERROR);
        }

        // 签到必须发生在号源的实际出诊时间段内，不能只校验“当天”。
        LocalDateTime visitStart = appointment.visitDate().atTime(appointment.startTime());
        LocalDateTime visitEnd = appointment.visitDate().atTime(appointment.endTime());
        LocalDateTime earliestSignIn = visitStart.minusMinutes(
                runtimeSystemConfigService.getSignInAheadMinutes()
        );
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(earliestSignIn) || !now.isBefore(visitEnd)) {
            throw new BusinessException(ErrorCode.SIGN_TIME_ERROR);
        }

        LocalDateTime signTime = LocalDateTime.now();

        SignIn signIn = new SignIn();
        signIn.setAppointmentId(appointmentId);
        signIn.setSignTime(signTime);
        signInMapper.insert(signIn);

        /*
         * 同一医生当天的排队号必须顺序递增。
         * 先锁定医生行，再读取最大序号，可避免并发签到生成相同号码。
         */
        signInMapper.lockDoctor(appointment.doctorId());
        int nextSequence = signInMapper.selectMaxQueueSequence(
                appointment.doctorId(),
                appointment.visitDate()
        ) + 1;
        String queueNo = String.format("A%03d", nextSequence);

        QueueRecord queue = new QueueRecord();
        queue.setAppointmentId(appointmentId);
        queue.setQueueNo(queueNo);
        queue.setSignTime(signTime);
        // 新签到患者默认排在现有队列末尾，后续异常调序仅修改 sortNo。
        queue.setSortNo(nextSequence);
        queue.setStatus(QUEUE_WAITING);
        queueMapper.insert(queue);

        if (signInMapper.markAppointmentSignedIn(appointmentId) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_PAY);
        }

        // 签到、排队和通知处于同一事务，保证患者看到的消息一定对应有效队列。
        messageNotificationService.createSignInSuccess(
                userId,
                appointmentId,
                queueNo
        );

        return new SignInResponse(queueNo);
    }
}
