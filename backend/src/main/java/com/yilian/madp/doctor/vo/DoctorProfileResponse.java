package com.yilian.madp.doctor.vo;

/** 当前登录医生的完整只读资料。 */
public record DoctorProfileResponse(
        Long doctorId,
        Long userId,
        String username,
        String role,
        String name,
        Long departmentId,
        String departmentName,
        String title,
        String introduction,
        String status,
        String doctorStatus
) {
}
