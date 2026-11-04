package com.yilian.madp.examination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.examination.entity.Examination;
import com.yilian.madp.examination.dto.DoctorExaminationQuery;
import com.yilian.madp.examination.vo.ExaminationLockResponse;
import com.yilian.madp.examination.vo.DoctorExaminationListItemResponse;
import com.yilian.madp.examination.vo.ExaminationReportResponse;
import com.yilian.madp.examination.vo.ExaminationMedicalRecordOwner;
import com.yilian.madp.examination.vo.PatientExaminationListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 检查申请及报告数据访问层。 */
@Mapper
public interface ExaminationMapper extends BaseMapper<Examination> {

    /** 校验诊疗记录归属，并取得需要接收通知的患者用户 ID。 */
    ExaminationMedicalRecordOwner selectOwnedMedicalRecord(
            @Param("userId") Long userId,
            @Param("medicalRecordId") Long medicalRecordId
    );

    int countDoctorExaminations(
            @Param("userId") Long userId,
            @Param("query") DoctorExaminationQuery query
    );

    List<DoctorExaminationListItemResponse> selectDoctorExaminations(
            @Param("userId") Long userId,
            @Param("query") DoctorExaminationQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    List<PatientExaminationListItemResponse> selectPatientExaminations(
            @Param("userId") Long userId
    );

    /** 锁定当前医生自己的检查申请，防止重复录入检查报告。 */
    ExaminationLockResponse selectOwnedExaminationForUpdate(
            @Param("userId") Long userId,
            @Param("examinationId") Long examinationId
    );

    ExaminationLockResponse selectPatientOwnedExaminationForUpdate(
            @Param("userId") Long userId,
            @Param("examinationId") Long examinationId
    );

    int markAwaitingDoctorReview(@Param("examinationId") Long examinationId);

    int markCompletedAfterReview(@Param("examinationId") Long examinationId);

    int markNeedsRevision(@Param("examinationId") Long examinationId);

    int markCompleted(@Param("examinationId") Long examinationId);

    ExaminationReportResponse selectPatientOwnedReport(
            @Param("userId") Long userId,
            @Param("examinationId") Long examinationId
    );

    ExaminationReportResponse selectDoctorOwnedReport(
            @Param("userId") Long userId,
            @Param("examinationId") Long examinationId
    );
}
