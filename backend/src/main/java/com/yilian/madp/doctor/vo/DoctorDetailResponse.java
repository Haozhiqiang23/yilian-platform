package com.yilian.madp.doctor.vo;

import lombok.Data;

/**
 * 医生详情返回对象，只包含患者端可以查看的公开信息。
 */
@Data
public class DoctorDetailResponse {

    private Long id;

    private String name;

    private Long departmentId;

    private String department;

    private String title;

    private String introduction;
}
