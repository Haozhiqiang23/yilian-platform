package com.yilian.madp.medicalrecord.service;

import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.medicalrecord.dto.DoctorMedicalRecordQuery;
import com.yilian.madp.medicalrecord.mapper.MedicalRecordMapper;
import com.yilian.madp.medicalrecord.vo.MedicalRecordListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 医生查看患者历史诊疗记录业务层。 */
@Service
@RequiredArgsConstructor
public class DoctorMedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;

    public PageResult<MedicalRecordListItemResponse> getPatientHistory(
            Long userId,
            Long patientId,
            DoctorMedicalRecordQuery query
    ) {
        /*
         * 先校验当前医生与患者是否存在真实诊疗关系。
         * 无关系和患者不存在都返回 404，避免暴露患者是否存在。
         */
        if (medicalRecordMapper.countDoctorPatientRelation(
                userId,
                patientId
        ) == 0) {
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        int total = medicalRecordMapper.countPatientHistory(patientId);
        int offset = (query.getPage() - 1) * query.getSize();
        List<MedicalRecordListItemResponse> records =
                medicalRecordMapper.selectPatientHistory(
                        patientId,
                        offset,
                        query.getSize()
                );

        return new PageResult<>(records, total);
    }
}
