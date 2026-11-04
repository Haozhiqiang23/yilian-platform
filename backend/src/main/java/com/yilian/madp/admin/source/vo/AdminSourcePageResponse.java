package com.yilian.madp.admin.source.vo;

import java.util.List;

/** 管理员号源分页结果，同时返回不受分页影响的概览统计。 */
public record AdminSourcePageResponse(
        List<AdminSourceListItemResponse> records,
        Integer total,
        AdminSourceOverviewResponse overview
) {
}
