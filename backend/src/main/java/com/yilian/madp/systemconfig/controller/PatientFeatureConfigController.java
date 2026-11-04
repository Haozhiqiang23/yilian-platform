package com.yilian.madp.systemconfig.controller;

import com.yilian.madp.common.Result;
import com.yilian.madp.systemconfig.service.RuntimeSystemConfigService;
import com.yilian.madp.systemconfig.vo.PatientFeatureConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 患者端读取功能开关的只读接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patients/config")
public class PatientFeatureConfigController {

    private final RuntimeSystemConfigService runtimeSystemConfigService;

    @GetMapping
    public Result<PatientFeatureConfigResponse> getConfig() {
        return Result.success(
                "查询患者端功能配置成功",
                new PatientFeatureConfigResponse(
                        runtimeSystemConfigService.isRefundEnabled(),
                        runtimeSystemConfigService.getAppointmentCancelHours(),
                        runtimeSystemConfigService.getSignInAheadMinutes(),
                        runtimeSystemConfigService.getQueuePollSeconds()
                )
        );
    }
}
