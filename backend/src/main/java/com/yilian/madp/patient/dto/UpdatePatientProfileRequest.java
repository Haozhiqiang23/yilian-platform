package com.yilian.madp.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改患者资料请求。
 *
 * 只声明允许患者修改的手机号和联系地址，
 * 姓名、身份证号等实名信息不会被客户端请求覆盖。
 */
public class UpdatePatientProfileRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    @Size(max = 120, message = "联系地址不能超过 120 个字符")
    private String address;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
