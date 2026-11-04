package com.yilian.madp.admin.doctor.vo;

/** 审核事务中锁定的医生账号信息。 */
public record DoctorAuditLockResponse(
        Long doctorId,
        Long userId,
        String name,
        String doctorStatus
) {
}
