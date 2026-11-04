package com.yilian.madp.queue.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import com.yilian.madp.queue.mapper.QueueMapper;
import com.yilian.madp.queue.vo.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 患者排队查询业务层。 */
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String QUEUE_WAITING = "等待接诊";

    private final QueueMapper queueMapper;
    private final PatientMapper patientMapper;

    public QueueStatusResponse getPatientQueue(
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

        QueueStatusResponse queue = queueMapper.selectOwnedQueue(
                patient.getId(),
                appointmentId
        );

        if (queue == null) {
            // 无队列和他人的队列统一返回不存在，避免泄露信息。
            throw new BusinessException(ErrorCode.QUEUE_NOT_FOUND);
        }

        if (QUEUE_WAITING.equals(queue.getStatus())) {
            queue.setRank(queueMapper.countWaitingAhead(
                    queue.getQueueId()
            ));
        } else {
            queue.setRank(0);
        }

        return queue;
    }
}
