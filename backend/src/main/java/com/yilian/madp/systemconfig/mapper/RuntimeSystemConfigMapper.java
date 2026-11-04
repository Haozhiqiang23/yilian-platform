package com.yilian.madp.systemconfig.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * 运行时系统配置查询。
 *
 * <p>这里只提供业务执行时需要的只读参数，
 * 不依赖管理端的配置修改模块，避免患者业务反向依赖 admin 包。</p>
 */
@Mapper
public interface RuntimeSystemConfigMapper {

    Boolean selectRefundEnabled();

    Integer selectAppointmentCancelHours();

    Integer selectSignInAheadMinutes();

    Integer selectQueuePollSeconds();
}
