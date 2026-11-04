package com.yilian.madp.admin.user.mapper;

import com.yilian.madp.admin.user.dto.AdminUserListQuery;
import com.yilian.madp.admin.user.vo.AdminUserListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理员用户管理数据查询。 */
@Mapper
public interface AdminUserMapper {

    int countUsers(@Param("query") AdminUserListQuery query);

    List<AdminUserListItemResponse> selectUsers(
            @Param("query") AdminUserListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 查询账号角色；不存在时返回 null。 */
    String selectRoleById(@Param("userId") Long userId);

    /** 用户管理中的医生账号需要转交医生状态服务统一处理。 */
    Long selectDoctorIdByUserId(@Param("userId") Long userId);

    /** 将接口英文状态转换为数据库中的中文状态后更新。 */
    int updateStatus(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    int insertStatusLog(
            @Param("adminId") Long adminId,
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("reason") String reason
    );
}
