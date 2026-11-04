package com.yilian.madp.doctorqueue.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 医生队列中的单个患者。 */
public record DoctorQueueItemResponse(
        Long queueId,
        Long appointmentId,
        Long patientId,
        String patientName,
        String queueNo,
        LocalDate visitDate,
        String timePeriod,
        String appointmentTime,
        LocalDateTime signInTime,
        String status,
        String queueStatus,
        Long medicalRecordId,
        String diagnosis,
        String treatment
) {
}
