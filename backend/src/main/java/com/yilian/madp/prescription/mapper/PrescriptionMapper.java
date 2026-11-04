package com.yilian.madp.prescription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.prescription.entity.Prescription;
import com.yilian.madp.prescription.dto.DoctorPrescriptionQuery;
import com.yilian.madp.prescription.dto.UpdatePrescriptionRequest;
import com.yilian.madp.prescription.vo.DoctorPrescriptionListItemResponse;
import com.yilian.madp.prescription.vo.PrescriptionDetailResponse;
import com.yilian.madp.prescription.vo.PrescriptionMedicalRecordOwner;
import com.yilian.madp.prescription.vo.PrescriptionLockResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 处方数据访问层。 */
@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {

    /** 查询当前医生拥有的诊疗记录，并取得接收通知的患者用户 ID。 */
    PrescriptionMedicalRecordOwner selectOwnedMedicalRecord(
            @Param("userId") Long userId,
            @Param("medicalRecordId") Long medicalRecordId
    );

    int countDoctorPrescriptions(
            @Param("userId") Long userId,
            @Param("query") DoctorPrescriptionQuery query
    );

    List<DoctorPrescriptionListItemResponse> selectDoctorPrescriptions(
            @Param("userId") Long userId,
            @Param("query") DoctorPrescriptionQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 锁定当前医生自己的处方，防止并发编辑造成状态覆盖。 */
    PrescriptionLockResponse selectOwnedPrescriptionForUpdate(
            @Param("userId") Long userId,
            @Param("prescriptionId") Long prescriptionId
    );

    int updateDraftPrescription(
            @Param("prescriptionId") Long prescriptionId,
            @Param("request") UpdatePrescriptionRequest request
    );

    PrescriptionDetailResponse selectPatientOwnedDetail(
            @Param("userId") Long userId,
            @Param("prescriptionId") Long prescriptionId
    );

    PrescriptionDetailResponse selectDoctorOwnedDetail(
            @Param("userId") Long userId,
            @Param("prescriptionId") Long prescriptionId
    );
}
