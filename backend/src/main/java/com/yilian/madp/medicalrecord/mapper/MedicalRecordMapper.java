package com.yilian.madp.medicalrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.medicalrecord.entity.MedicalRecord;
import com.yilian.madp.medicalrecord.vo.MedicalRecordListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 诊疗记录数据访问层。 */
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {

    int countMine(@Param("patientId") Long patientId);

    List<MedicalRecordListItemResponse> selectMine(
            @Param("patientId") Long patientId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countDoctorPatientRelation(
            @Param("userId") Long userId,
            @Param("patientId") Long patientId
    );

    int countPatientHistory(@Param("patientId") Long patientId);

    List<MedicalRecordListItemResponse> selectPatientHistory(
            @Param("patientId") Long patientId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
