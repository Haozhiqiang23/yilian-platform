package com.yilian.madp.admin.statistics.mapper;

import com.yilian.madp.admin.statistics.vo.AdminStatisticsResponse;
import org.apache.ibatis.annotations.Mapper;

/** 管理员首页统计查询。 */
@Mapper
public interface AdminStatisticsMapper {

    /**
     * 一次查询返回首页需要的全部统计值，减少多次访问数据库的开销。
     */
    AdminStatisticsResponse selectOverview();
}
