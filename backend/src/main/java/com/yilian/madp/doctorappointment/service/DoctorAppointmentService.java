package com.yilian.madp.doctorappointment.service;

import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.doctorappointment.mapper.DoctorAppointmentMapper;
import com.yilian.madp.doctorappointment.vo.DoctorAppointmentLockResponse;
import com.yilian.madp.doctorappointment.vo.DoctorAppointmentStatusResponse;
import com.yilian.madp.doctorappointment.vo.DoctorQueueLockResponse;
import com.yilian.madp.doctorappointment.dto.FinishAppointmentRequest;
import com.yilian.madp.doctorappointment.vo.FinishAppointmentResponse;
import com.yilian.madp.medicalrecord.entity.MedicalRecord;
import com.yilian.madp.medicalrecord.mapper.MedicalRecordMapper;
import com.yilian.madp.message.service.MessageNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 医生开始和完成接诊的业务层。 */
@Service
@RequiredArgsConstructor
public class DoctorAppointmentService {

    private static final String APPOINTMENT_SIGNED_IN = "已签到";
    private static final String APPOINTMENT_IN_CONSULTATION = "接诊中";
    private static final String APPOINTMENT_COMPLETED = "已完成";
    private static final String PAYMENT_SUCCESS = "支付成功";
    private static final String QUEUE_WAITING = "等待接诊";
    private static final String QUEUE_IN_CONSULTATION = "接诊中";

    private final DoctorAppointmentMapper doctorAppointmentMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MessageNotificationService messageNotificationService;

    /**
     * 开始接诊时同步修改预约和队列状态。
     * 两行数据先加锁再进行条件更新，可防止医生重复点击造成并发状态错乱。
     */
    @Transactional
    public DoctorAppointmentStatusResponse start(
            Long userId,
            Long appointmentId
    ) {
        DoctorAppointmentLockResponse appointment =
                doctorAppointmentMapper.selectOwnedAppointmentForUpdate(
                        userId,
                        appointmentId
                );
        if (appointment == null) {
            // 不区分预约不存在和属于其他医生，避免泄露其他医生的患者信息。
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }

        if (!APPOINTMENT_SIGNED_IN.equals(appointment.status())
                || !PAYMENT_SUCCESS.equals(appointment.paymentStatus())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        DoctorQueueLockResponse queue =
                doctorAppointmentMapper.selectQueueForUpdate(appointmentId);
        if (queue == null) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_FOUND);
        }
        if (!QUEUE_WAITING.equals(queue.status())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        if (doctorAppointmentMapper.markAppointmentInConsultation(
                appointmentId
        ) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        if (doctorAppointmentMapper.markQueueInConsultation(
                queue.queueId()
        ) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        return new DoctorAppointmentStatusResponse(
                appointmentId,
                APPOINTMENT_IN_CONSULTATION
        );
    }

    /**
     * 完成接诊、生成诊疗记录、结束队列并通知患者。
     * 这些操作必须全部成功；任何一步异常都会由事务统一回滚。
     */
    @Transactional
    public FinishAppointmentResponse finish(
            Long userId,
            Long appointmentId,
            FinishAppointmentRequest request
    ) {
        DoctorAppointmentLockResponse appointment =
                doctorAppointmentMapper.selectOwnedAppointmentForUpdate(
                        userId,
                        appointmentId
                );
        if (appointment == null) {
            throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND);
        }
        if (!APPOINTMENT_IN_CONSULTATION.equals(appointment.status())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        DoctorQueueLockResponse queue =
                doctorAppointmentMapper.selectQueueForUpdate(appointmentId);
        if (queue == null) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_FOUND);
        }
        if (!QUEUE_IN_CONSULTATION.equals(queue.status())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        MedicalRecord record = new MedicalRecord();
        record.setAppointmentId(appointmentId);
        record.setDoctorId(appointment.doctorId());
        record.setPatientId(appointment.patientId());
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setTreatment(request.getTreatment().trim());

        try {
            medicalRecordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            // appointment_id 唯一索引是防止并发生成重复诊疗记录的最终保护。
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_EXIST);
        }
        if (doctorAppointmentMapper.markAppointmentCompleted(
                appointmentId
        ) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }
        if (doctorAppointmentMapper.markQueueCompleted(queue.queueId()) != 1) {
            throw new BusinessException(ErrorCode.APPOINTMENT_STATUS_ERROR);
        }

        messageNotificationService.createConsultationCompleted(
                appointment.patientUserId(),
                appointmentId,
                record.getId()
        );

        return new FinishAppointmentResponse(record.getId());
    }
}
