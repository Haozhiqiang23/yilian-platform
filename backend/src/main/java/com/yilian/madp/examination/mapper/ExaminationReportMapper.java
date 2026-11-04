package com.yilian.madp.examination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.examination.entity.ExaminationReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 检查报告写入数据访问层。 */
@Mapper
public interface ExaminationReportMapper extends BaseMapper<ExaminationReport> {

    @Select("SELECT id, examination_id, result, remark, create_time "
            + "FROM examination_report WHERE examination_id = #{examinationId}")
    ExaminationReport selectByExaminationId(Long examinationId);
}
