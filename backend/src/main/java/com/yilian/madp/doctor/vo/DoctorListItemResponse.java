package com.yilian.madp.doctor.vo;

import lombok.Data;

/**
 * 医生列表单项返回对象。
 *
 * department 和 remainCount 来自关联查询，不属于 doctor 表本身，
 * 因此放在 VO 中而不是 Doctor Entity 中。
 */
@Data
public class DoctorListItemResponse {

    private Long id;

    private String name;

    private Long departmentId;

    private String department;

    private String title;

    private String introduction;

    private Integer remainCount;
}
