package com.yilian.madp.admin.source.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/** 管理员创建号源请求。 */
public class CreateAdminSourceRequest {
    @NotNull(message = "请选择医生")
    private Long doctorId;

    @NotNull(message = "请选择出诊日期")
    @FutureOrPresent(message = "出诊日期不能早于今天")
    private LocalDate visitDate;

    @NotNull(message = "请选择开始时间")
    private LocalTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalTime endTime;

    @NotNull(message = "请输入总号数")
    @Min(value = 1, message = "总号数必须大于0")
    private Integer totalCount;

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

}
