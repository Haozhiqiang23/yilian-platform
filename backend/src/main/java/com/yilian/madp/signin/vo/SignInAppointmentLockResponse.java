package com.yilian.madp.signin.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/** 签到校验所需的预约、医生和就诊时段。 */
public record SignInAppointmentLockResponse(
        Long appointmentId,
        Long doctorId,
        String status,
        LocalDate visitDate,
        String timePeriod,
        LocalTime startTime,
        LocalTime endTime
) {
}
