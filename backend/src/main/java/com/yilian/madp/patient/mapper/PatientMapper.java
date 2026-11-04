package com.yilian.madp.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.patient.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 患者数据访问层，只负责执行与 patient 表有关的 SQL。
 */
@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

    /**
     * 根据系统用户 ID 查询对应的患者资料。
     *
     * sys_user.id 是登录身份，patient.user_id 用来关联患者业务资料。
     */
    @Select("""
            SELECT id, user_id, name, id_card, phone, address, create_time
            FROM patient
            WHERE user_id = #{userId}
            """)
    Patient selectByUserId(Long userId);

    /** 查询身份证号是否已被其他患者使用。 */
    @Select("SELECT COUNT(1) FROM patient WHERE id_card = #{idCard}")
    long countByIdCard(@Param("idCard") String idCard);

}
