package com.yilian.madp.admin.user.vo;

import java.time.LocalDateTime;

/** 管理员用户列表中的一条账号信息，不包含密码。 */
public record AdminUserListItemResponse(
        Long id,
        String username,
        String name,
        String phone,
        String role,
        String status,
        LocalDateTime createTime
) {
}
