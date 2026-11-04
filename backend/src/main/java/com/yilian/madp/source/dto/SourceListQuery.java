package com.yilian.madp.source.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 号源查询参数，doctorId 必填，date 可选。
 */
public class SourceListQuery {

    @NotNull(message = "doctorId不能为空")
    @Positive(message = "doctorId必须大于0")
    private Long doctorId;

//    前端传来的日期字符串，要按照标准日期格式转换成 LocalDate。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
