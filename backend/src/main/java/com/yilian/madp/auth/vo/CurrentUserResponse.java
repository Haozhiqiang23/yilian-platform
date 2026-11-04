package com.yilian.madp.auth.vo;

/**
 * 当前登录用户的基础身份信息。
 */
public class CurrentUserResponse {

    private final Long userId;
    private final String role;

    public CurrentUserResponse(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
