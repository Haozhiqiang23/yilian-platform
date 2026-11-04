package com.yilian.madp.admin.systemconfig.service;

import com.yilian.madp.admin.systemconfig.dto.UpdateSystemConfigRequest;
import com.yilian.madp.admin.systemconfig.mapper.AdminSystemConfigMapper;
import com.yilian.madp.admin.systemconfig.vo.SystemConfigResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员系统配置业务层。 */
@Service
@RequiredArgsConstructor
public class AdminSystemConfigService {

    private final AdminSystemConfigMapper adminSystemConfigMapper;

    /** 读取平台唯一一条配置。 */
    public SystemConfigResponse getConfig() {
        SystemConfigResponse config = adminSystemConfigMapper.selectConfig();
        if (config == null) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        return config;
    }

    /**
     * 配置更新与审计日志必须同时成功或同时回滚，避免参数已改变但查不到操作记录。
     */
    @Transactional
    public SystemConfigResponse updateConfig(
            Long adminId,
            UpdateSystemConfigRequest request
    ) {
        if (adminSystemConfigMapper.updateConfig(request, adminId) != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
        }
        adminSystemConfigMapper.insertUpdateLog(adminId);
        return getConfig();
    }

}
