package com.yilian.madp.prescription.service;

import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.message.service.MessageNotificationService;
import com.yilian.madp.prescription.dto.CreatePrescriptionRequest;
import com.yilian.madp.prescription.dto.DoctorPrescriptionQuery;
import com.yilian.madp.prescription.dto.UpdatePrescriptionRequest;
import com.yilian.madp.prescription.entity.Prescription;
import com.yilian.madp.prescription.mapper.PrescriptionMapper;
import com.yilian.madp.prescription.vo.CreatePrescriptionResponse;
import com.yilian.madp.prescription.vo.DoctorPrescriptionListItemResponse;
import com.yilian.madp.prescription.vo.PrescriptionDetailResponse;
import com.yilian.madp.prescription.vo.PrescriptionMedicalRecordOwner;
import com.yilian.madp.prescription.vo.PrescriptionLockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 处方详情业务层。 */
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final String STATUS_DRAFT = "草稿";

    private final PrescriptionMapper prescriptionMapper;
    private final MessageNotificationService messageNotificationService;

    /** 查询当前登录医生开具的处方，不接收前端传入的 doctorId。 */
    public PageResult<DoctorPrescriptionListItemResponse> getDoctorPrescriptions(
            Long userId,
            DoctorPrescriptionQuery query
    ) {
        normalizeStatus(query);
        int total = prescriptionMapper.countDoctorPrescriptions(userId, query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<DoctorPrescriptionListItemResponse> records =
                prescriptionMapper.selectDoctorPrescriptions(
                        userId,
                        query,
                        offset,
                        query.getSize()
                );
        return new PageResult<>(records, total);
    }

    /**
     * 为当前医生创建的诊疗记录开具处方。
     * 处方和患者通知处于同一事务中，任一步失败都会整体回滚。
     */
    @Transactional
    public CreatePrescriptionResponse create(
            Long userId,
            CreatePrescriptionRequest request
    ) {
        PrescriptionMedicalRecordOwner owner =
                prescriptionMapper.selectOwnedMedicalRecord(
                        userId,
                        request.getMedicalRecordId()
                );
        if (owner == null) {
            // 不区分“不存在”和“属于其他医生”，避免泄露诊疗隐私。
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        Prescription prescription = new Prescription();
        prescription.setMedicalRecordId(owner.medicalRecordId());
        prescription.setStatus(STATUS_DRAFT);
        prescription.setMedicineInfo(request.getMedicineInfo().trim());
        prescription.setUsageInfo(request.getUsageInfo().trim());
        prescription.setAmount(request.getAmount());
        prescriptionMapper.insert(prescription);

        messageNotificationService.createPrescriptionGenerated(
                owner.patientUserId(),
                prescription.getId(),
                owner.medicalRecordId()
        );

        return new CreatePrescriptionResponse(
                prescription.getId(),
                owner.medicalRecordId(),
                prescription.getStatus(),
                prescription.getAmount()
        );
    }

    /**
     * 编辑处方草稿，允许保存草稿或提交。
     * 已提交处方视为医疗业务凭证，本版本不允许直接覆盖修改。
     */
    @Transactional
    public PrescriptionDetailResponse update(
            Long userId,
            Long prescriptionId,
            UpdatePrescriptionRequest request
    ) {
        PrescriptionLockResponse prescription =
                prescriptionMapper.selectOwnedPrescriptionForUpdate(
                        userId,
                        prescriptionId
                );
        if (prescription == null) {
            // 不区分不存在和属于其他医生，避免泄露其他医生的处方。
            throw new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND);
        }
        if (!STATUS_DRAFT.equals(prescription.status())) {
            throw new BusinessException(ErrorCode.PRESCRIPTION_STATUS_ERROR);
        }

        request.setMedicineInfo(request.getMedicineInfo().trim());
        request.setUsageInfo(request.getUsageInfo().trim());
        request.setStatus(request.getStatus().trim());

        if (prescriptionMapper.updateDraftPrescription(
                prescriptionId,
                request
        ) != 1) {
            throw new BusinessException(ErrorCode.PRESCRIPTION_STATUS_ERROR);
        }

        return prescriptionMapper.selectDoctorOwnedDetail(
                userId,
                prescriptionId
        );
    }

    /**
     * 根据当前登录角色选择带有归属条件的 SQL。
     * 归属校验直接放在查询条件中，避免先查询处方后再判断造成隐私泄露。
     */
    public PrescriptionDetailResponse getDetail(
            Long prescriptionId,
            CurrentUserResponse currentUser
    ) {

        PrescriptionDetailResponse detail;

        if ("patient".equals(currentUser.getRole())) {
            detail = prescriptionMapper.selectPatientOwnedDetail(
                    currentUser.getUserId(),
                    prescriptionId
            );
        } else if ("doctor".equals(currentUser.getRole())) {
            detail = prescriptionMapper.selectDoctorOwnedDetail(
                    currentUser.getUserId(),
                    prescriptionId
            );
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 不区分“处方不存在”和“处方不属于当前用户”，防止泄露其他人的处方信息。
        if (detail == null) {
            throw new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND);
        }

        return detail;
    }

    private void normalizeStatus(DoctorPrescriptionQuery query) {
        if (query.getStatus() != null) {
            String status = query.getStatus().trim();
            query.setStatus(status.isEmpty() ? null : status);
        }
    }
}
