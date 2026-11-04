package com.yilian.madp.admin.user.vo;

/** 用户状态修改结果。 */
public record AdminUserStatusResponse(
        Long userId,
        String status
) {
}
