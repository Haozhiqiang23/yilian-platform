package com.yilian.madp.admin.source.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** 管理员调整号源请求；未提交的字段保持原值。 */
public class UpdateAdminSourceRequest {

    @FutureOrPresent(message = "出诊日期不能早于今天")
    private LocalDate visitDate;

    @Pattern(regexp = "上午|下午", message = "出诊时段只能是上午或下午")
    private String timePeriod;

    /** 新字段：出诊开始时间。与 endTime 成对提交。 */
    private LocalTime startTime;

    /** 新字段：出诊结束时间。必须晚于 startTime。 */
    private LocalTime endTime;

    @Min(value = 1, message = "总号数必须大于0")
    private Integer totalCount;

    @Size(max = 100, message = "调整原因不能超过100个字符")
    private String reason;

    @AssertTrue(message = "至少需要提交一项号源调整内容")
    public boolean isAdjustmentPresent() {
        return visitDate != null || timePeriod != null || startTime != null
                || endTime != null || totalCount != null;
    }

    @AssertTrue(message = "出诊开始时间和结束时间必须同时填写，且结束时间晚于开始时间")
    public boolean isTimeRangeValid() {
        if (startTime == null && endTime == null) {
            return true;
        }
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public String getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
