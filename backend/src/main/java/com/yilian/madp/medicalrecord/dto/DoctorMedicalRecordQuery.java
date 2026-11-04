package com.yilian.madp.medicalrecord.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 医生查看患者历史诊疗记录的分页参数。 */
public class DoctorMedicalRecordQuery {

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
