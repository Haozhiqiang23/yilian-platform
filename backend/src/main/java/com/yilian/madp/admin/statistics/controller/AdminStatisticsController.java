package com.yilian.madp.admin.statistics.controller;

import com.yilian.madp.admin.statistics.service.AdminStatisticsService;
import com.yilian.madp.admin.statistics.vo.AdminStatisticsResponse;
import com.yilian.madp.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员后台首页统计接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/statistics")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping
    public Result<AdminStatisticsResponse> getOverview() {
        return Result.success(
                "查询管理员统计成功",
                adminStatisticsService.getOverview()
        );
    }
}
