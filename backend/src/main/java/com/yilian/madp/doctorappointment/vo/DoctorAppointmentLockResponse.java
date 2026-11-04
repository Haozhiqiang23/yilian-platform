package com.yilian.madp.doctorappointment.vo;

/** 医生操作预约时锁定读取的预约和支付状态。 */
public record DoctorAppointmentLockResponse(
        Long appointmentId,
        Long doctorId,
        Long patientId,
        Long patientUserId,
        String status,
        String paymentStatus
) {
}
