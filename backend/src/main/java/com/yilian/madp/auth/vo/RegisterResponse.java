package com.yilian.madp.auth.vo;

/**
 * 注册成功后返回的基础信息，不返回密码和身份证号。
 */
public class RegisterResponse {

    private final Long userId;
    private final String phone;
    private final String name;

    public RegisterResponse(Long userId, String phone, String name) {
        this.userId = userId;
        this.phone = phone;
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }
}
