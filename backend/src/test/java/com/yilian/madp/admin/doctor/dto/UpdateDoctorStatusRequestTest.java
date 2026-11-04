package com.yilian.madp.admin.doctor.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateDoctorStatusRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void disableMustProvideReason() {
        UpdateDoctorStatusRequest request = requestOf("DISABLED", "  ");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("禁用医生账号时必须填写原因");
    }

    @Test
    void disableWithReasonIsValid() {
        UpdateDoctorStatusRequest request = requestOf("DISABLED", "账号存在异常操作");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void restoreDoesNotRequireReason() {
        UpdateDoctorStatusRequest request = requestOf("NORMAL", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    private static UpdateDoctorStatusRequest requestOf(String status, String reason) {
        UpdateDoctorStatusRequest request = new UpdateDoctorStatusRequest();
        request.setStatus(status);
        request.setReason(reason);
        return request;
    }
}
