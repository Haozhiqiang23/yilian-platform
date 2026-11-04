package com.yilian.madp.admin.doctor.vo;

import java.time.LocalDateTime;

/** 管理员查看的医生完整资料，包含登录账号与业务状态。 */
public record AdminDoctorDetailResponse(
        Long doctorId,
        Long userId,
        String username,
        String name,
        Long departmentId,
        String department,
        String title,
        String introduction,
        String accountStatus,
        String doctorStatus,
        String disableReason,
        LocalDateTime disableTime,
        Long disabledBy,
        String disabledByName,
        LocalDateTime createTime
) {
}
