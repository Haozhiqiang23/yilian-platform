package com.yilian.madp.doctorappointment.vo;

/** 完成接诊后返回新生成的诊疗记录 ID。 */
public record FinishAppointmentResponse(
        Long medicalRecordId
) {
}
