package com.yilian.madp.patient.vo;

/**
 * 返回给前端的患者个人信息。
 *
 * VO 只暴露页面需要的数据，不返回 userId、createTime 等内部字段。
 */
public class PatientProfileResponse {

    private final Long id;
    private final String name;
    private final String phone;
    private final String idCard;
    private final String address;

    public PatientProfileResponse(
            Long id,
            String name,
            String phone,
            String idCard,
            String address
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.idCard = idCard;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public String getAddress() {
        return address;
    }
}
