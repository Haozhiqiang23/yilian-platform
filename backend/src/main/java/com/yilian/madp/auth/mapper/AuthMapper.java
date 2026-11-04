package com.yilian.madp.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT id, username, password, role, status, create_time
            FROM sys_user
            WHERE username = #{username}
            """)
    SysUser selectByUsername(String username);

}
