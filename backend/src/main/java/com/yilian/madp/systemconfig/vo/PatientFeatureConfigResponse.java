package com.yilian.madp.systemconfig.vo;

/** 患者端可感知的功能开关，不向患者暴露管理端配置细节。 */
public record PatientFeatureConfigResponse(
        Boolean refundEnabled,
        Integer appointmentCancelHours,
        Integer signInAheadMinutes,
        Integer queuePollSeconds
) {
}
