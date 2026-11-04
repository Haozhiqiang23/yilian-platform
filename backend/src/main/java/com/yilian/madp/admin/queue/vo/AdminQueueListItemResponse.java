package com.yilian.madp.admin.queue.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 管理员查看的单条排队记录。 */
public record AdminQueueListItemResponse(
        Long queueId,
        Long appointmentId,
        Long doctorId,
        String doctorName,
        Long patientId,
        String patientName,
        String queueNo,
        Integer position,
        String status,
        LocalDate visitDate,
        String timePeriod,
        LocalDateTime signInTime
) {
}
