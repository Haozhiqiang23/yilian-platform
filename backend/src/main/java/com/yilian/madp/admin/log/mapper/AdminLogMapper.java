package com.yilian.madp.admin.log.mapper;

import com.yilian.madp.admin.log.dto.AdminLogListQuery;
import com.yilian.madp.admin.log.vo.AdminLogListItemResponse;
import com.yilian.madp.admin.log.vo.AdminLogDetailResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理员操作日志数据访问层。 */
@Mapper
public interface AdminLogMapper {

    int countLogs(@Param("query") AdminLogListQuery query);

    List<AdminLogListItemResponse> selectLogs(
            @Param("query") AdminLogListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    AdminLogDetailResponse selectLogById(@Param("id") Long id);
}
