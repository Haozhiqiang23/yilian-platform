package com.yilian.madp.systemconfig.service;

import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.systemconfig.mapper.RuntimeSystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 为各业务端提供已生效的系统参数。 */
@Service
@RequiredArgsConstructor
public class RuntimeSystemConfigService {

    private final RuntimeSystemConfigMapper runtimeSystemConfigMapper;

    /**
     * 查询退款申请入口是否开放。
     * 配置行缺失时不默认放行，防止在管理员无法控制的情况下创建退款申请。
     */
    public boolean isRefundEnabled() {
        Boolean enabled = runtimeSystemConfigMapper.selectRefundEnabled();
        if (enabled == null) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        return enabled;
    }

    /** 已支付预约进入退款流程前必须在后端再次校验，不能只依赖前端禁用按钮。 */
    public void checkRefundEnabled() {
        if (!isRefundEnabled()) {
            throw new BusinessException(ErrorCode.REFUND_DISABLED);
        }
    }

    /** 读取患者取消预约的提前时间（小时）。 */
    public int getAppointmentCancelHours() {
        Integer hours = runtimeSystemConfigMapper.selectAppointmentCancelHours();
        if (hours == null || hours < 0) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        return hours;
    }

    /** 读取允许患者提前多少分钟签到。 */
    public int getSignInAheadMinutes() {
        Integer minutes = runtimeSystemConfigMapper.selectSignInAheadMinutes();
        if (minutes == null || minutes < 0) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        return minutes;
    }

    /** 读取患者排队页面的自动刷新间隔（秒）。 */
    public int getQueuePollSeconds() {
        Integer seconds = runtimeSystemConfigMapper.selectQueuePollSeconds();
        if (seconds == null || seconds < 5) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        return seconds;
    }
}
