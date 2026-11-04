package com.yilian.madp.medicalrecord.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.medicalrecord.dto.MedicalRecordListQuery;
import com.yilian.madp.medicalrecord.mapper.MedicalRecordMapper;
import com.yilian.madp.medicalrecord.vo.MedicalRecordListItemResponse;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 患者诊疗记录查询业务层。 */
@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientMapper patientMapper;

    /**
     * 只根据当前登录用户对应的患者 ID 查询，患者 ID 不由前端传入。
     */
    public PageResult<MedicalRecordListItemResponse> getMyRecords(
            Long userId,
            MedicalRecordListQuery query
    ) {
        Patient patient = patientMapper.selectOne(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getUserId, userId)
        );
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_PATIENT);
        }

        int total = medicalRecordMapper.countMine(patient.getId());
        int offset = (query.getPage() - 1) * query.getSize();

        List<MedicalRecordListItemResponse> records =
                medicalRecordMapper.selectMine(
                        patient.getId(),
                        offset,
                        query.getSize()
                );

        return new PageResult<>(records, total);
    }
}
