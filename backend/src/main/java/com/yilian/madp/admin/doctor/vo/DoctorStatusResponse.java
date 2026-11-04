package com.yilian.madp.admin.doctor.vo;

/** 管理员修改医生状态后的结果。 */
public record DoctorStatusResponse(
        Long doctorId,
        String doctorStatus,
        String accountStatus
) {
}
