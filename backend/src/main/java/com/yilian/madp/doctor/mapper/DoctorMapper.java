package com.yilian.madp.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.doctor.dto.DoctorListQuery;
import com.yilian.madp.doctor.entity.Doctor;
import com.yilian.madp.doctor.vo.DoctorDetailResponse;
import com.yilian.madp.doctor.vo.DoctorListItemResponse;
import com.yilian.madp.doctor.vo.DoctorProfileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 医生数据访问层。
 * 动态筛选、科室关联和号源汇总 SQL 放在 DoctorMapper.xml 中。
 */
@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    /** 统计符合筛选条件的启用医生数量。 */
    int countEnabled(@Param("query") DoctorListQuery query);

    /** 分页查询医生公开信息以及剩余号源数量。 */
    List<DoctorListItemResponse> selectEnabledPage(
            @Param("query") DoctorListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 根据医生 ID 查询启用医生的公开详情。
     * 需要关联科室，因此具体 SQL 放在 XML 中。
     */
    DoctorDetailResponse selectEnabledDetail(
            @Param("doctorId") Long doctorId
    );

    /** 根据 JWT 中的用户 ID 查询当前医生资料。 */
    DoctorProfileResponse selectProfileByUserId(
            @Param("userId") Long userId
    );

}
