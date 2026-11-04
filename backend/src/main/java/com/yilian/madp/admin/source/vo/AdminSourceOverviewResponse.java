package com.yilian.madp.admin.source.vo;

/**
 * 管理员号源概览。
 *
 * <p>统计范围与号源列表使用相同的医生姓名、出诊日期筛选条件，
 * 但不受分页参数影响。</p>
 */
public record AdminSourceOverviewResponse(
        Integer currentSources,
        Integer totalCount,
        Integer remainCount,
        Integer lowRemain
) {
}
