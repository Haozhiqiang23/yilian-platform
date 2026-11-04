package com.yilian.madp.source.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.source.dto.SourceListQuery;
import com.yilian.madp.source.entity.AppointmentSource;
import com.yilian.madp.source.vo.SourceResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 号源数据访问层，复杂查询放在 SourceMapper.xml 中。
 */
@Mapper
public interface SourceMapper extends BaseMapper<AppointmentSource> {




    /** 根据医生及可选日期查询号源。 */
    List<SourceResponse> selectSources(
            @Param("query") SourceListQuery query
    );
}
