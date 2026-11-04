package com.yilian.madp.admin.statistics.service;

import com.yilian.madp.admin.statistics.mapper.AdminStatisticsMapper;
import com.yilian.madp.admin.statistics.vo.AdminStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 管理员首页统计业务层。 */
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final AdminStatisticsMapper adminStatisticsMapper;

    /** 查询当前平台概览和待办数量。 */
    public AdminStatisticsResponse getOverview() {
        return adminStatisticsMapper.selectOverview();
    }
}
