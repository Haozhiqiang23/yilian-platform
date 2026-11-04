package com.yilian.madp.admin.statistics.vo;

/**
 * 管理员首页统计数据。
 *
 * <p>字段名称与当前 Vue 管理员首页保持一致，前端可以直接展示。</p>
 */
public record AdminStatisticsResponse(
        Integer totalUsers,
        Integer totalDoctors,
        Integer todayAppointments,
        Integer todayCompleted,
        Integer pendingRefundCount,
        Integer pendingAuditDoctorCount
) {
}
