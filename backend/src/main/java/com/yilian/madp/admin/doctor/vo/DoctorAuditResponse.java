package com.yilian.madp.admin.doctor.vo;

/** 医生审核操作结果。 */
public record DoctorAuditResponse(
        Long doctorId,
        String result,
        String doctorStatus,
        String userStatus
) {
}
