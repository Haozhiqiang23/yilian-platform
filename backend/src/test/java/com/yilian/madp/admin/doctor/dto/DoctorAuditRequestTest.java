package com.yilian.madp.admin.doctor.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DoctorAuditRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectMustProvideReason() {
        DoctorAuditRequest request = requestOf("REJECT", "   ");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("拒绝医生审核时必须填写原因");
    }

    @Test
    void rejectWithReasonIsValid() {
        DoctorAuditRequest request = requestOf("REJECT", "执业证书照片不清晰");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void passDoesNotRequireReason() {
        DoctorAuditRequest request = requestOf("PASS", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    private static DoctorAuditRequest requestOf(String result, String reason) {
        DoctorAuditRequest request = new DoctorAuditRequest();
        request.setResult(result);
        request.setReason(reason);
        return request;
    }
}
