package com.yilian.madp.admin.doctor.vo;

import java.time.LocalDateTime;

/** 管理员医生列表数据，同时区分账号状态和医生业务状态。 */
public record AdminDoctorListItemResponse(
        Long doctorId,
        Long userId,
        String username,
        String name,
        Long departmentId,
        String department,
        String title,
        String introduction,
        String status,
        String doctorStatus,
        String auditStatus,
        LocalDateTime createTime
) {
}
