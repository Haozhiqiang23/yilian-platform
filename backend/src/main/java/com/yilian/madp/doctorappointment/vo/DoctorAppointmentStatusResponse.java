package com.yilian.madp.doctorappointment.vo;

/** 医生改变接诊状态后的返回数据。 */
public record DoctorAppointmentStatusResponse(
        Long appointmentId,
        String status
) {
}
