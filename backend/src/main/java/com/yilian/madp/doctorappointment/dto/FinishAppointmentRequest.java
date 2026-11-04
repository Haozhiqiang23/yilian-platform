package com.yilian.madp.doctorappointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 医生完成接诊时填写的诊断和治疗建议。 */
public class FinishAppointmentRequest {

    @NotBlank(message = "初步诊断不能为空")
    @Size(max = 2000, message = "初步诊断不能超过2000个字符")
    private String diagnosis;

    @NotBlank(message = "治疗建议不能为空")
    @Size(max = 2000, message = "治疗建议不能超过2000个字符")
    private String treatment;

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }
}
