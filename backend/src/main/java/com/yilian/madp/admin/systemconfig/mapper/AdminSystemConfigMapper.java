package com.yilian.madp.admin.systemconfig.mapper;

import com.yilian.madp.admin.systemconfig.dto.UpdateSystemConfigRequest;
import com.yilian.madp.admin.systemconfig.vo.SystemConfigResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 系统配置数据访问层。平台当前采用单行配置，固定主键为1。 */
@Mapper
public interface AdminSystemConfigMapper {

    SystemConfigResponse selectConfig();

    int updateConfig(
            @Param("request") UpdateSystemConfigRequest request,
            @Param("adminId") Long adminId
    );

    int insertUpdateLog(@Param("adminId") Long adminId);
}
