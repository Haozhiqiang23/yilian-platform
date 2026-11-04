package com.yilian.madp.examination.service;

import com.yilian.madp.auth.vo.CurrentUserResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.examination.dto.CreateExaminationRequest;
import com.yilian.madp.examination.dto.CompleteExaminationRequest;
import com.yilian.madp.examination.dto.DoctorExaminationQuery;
import com.yilian.madp.examination.dto.RejectExaminationRequest;
import com.yilian.madp.examination.dto.SubmitExaminationResultRequest;
import com.yilian.madp.examination.entity.Examination;
import com.yilian.madp.examination.entity.ExaminationReport;
import com.yilian.madp.examination.mapper.ExaminationMapper;
import com.yilian.madp.examination.mapper.ExaminationReportMapper;
import com.yilian.madp.examination.vo.CreateExaminationResponse;
import com.yilian.madp.examination.vo.DoctorExaminationListItemResponse;
import com.yilian.madp.examination.vo.ExaminationMedicalRecordOwner;
import com.yilian.madp.examination.vo.ExaminationLockResponse;
import com.yilian.madp.examination.vo.ExaminationReportResponse;
import com.yilian.madp.examination.vo.PatientExaminationListItemResponse;
import com.yilian.madp.message.service.MessageNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

/** 检查报告业务层。 */
@Service
@RequiredArgsConstructor
public class ExaminationService {

    private static final String STATUS_APPLIED = "申请";
    private static final String STATUS_AWAITING_REVIEW = "待医生确认";
    private static final String STATUS_NEEDS_REVISION = "需补充";

    private final ExaminationMapper examinationMapper;
    private final ExaminationReportMapper examinationReportMapper;
    private final MessageNotificationService messageNotificationService;

    /** 查询当前登录医生创建的检查申请。 */
    public PageResult<DoctorExaminationListItemResponse> getDoctorExaminations(
            Long userId,
            DoctorExaminationQuery query
    ) {
        normalizeStatus(query);
        int total = examinationMapper.countDoctorExaminations(userId, query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<DoctorExaminationListItemResponse> records =
                examinationMapper.selectDoctorExaminations(
                        userId,
                        query,
                        offset,
                        query.getSize()
                );
        return new PageResult<>(records, total);
    }

    /** 查询当前患者本人名下的全部检查申请。 */
    public List<PatientExaminationListItemResponse> getPatientExaminations(Long userId) {
        return examinationMapper.selectPatientExaminations(userId);
    }

    /**
     * 在当前医生自己的诊疗记录下创建检查申请。
     * 检查申请和患者通知加入同一事务，避免只成功其中一项。
     */
    @Transactional
    public CreateExaminationResponse create(
            Long userId,
            CreateExaminationRequest request
    ) {
        ExaminationMedicalRecordOwner owner =
                examinationMapper.selectOwnedMedicalRecord(
                        userId,
                        request.getMedicalRecordId()
                );
        if (owner == null) {
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        Examination examination = new Examination();
        examination.setMedicalRecordId(owner.medicalRecordId());
        examination.setItemName(request.getItemName().trim());
        examination.setStatus(STATUS_APPLIED);
        examinationMapper.insert(examination);

        messageNotificationService.createExaminationRequested(
                owner.patientUserId(),
                examination.getId(),
                examination.getItemName()
        );

        return new CreateExaminationResponse(
                examination.getId(),
                owner.medicalRecordId(),
                examination.getItemName(),
                examination.getStatus()
        );
    }

    /**
     * 录入检查结果并将检查状态推进为完成。
     * 锁、唯一索引和事务共同防止同一检查生成两份报告。
     */
    @Transactional
    public ExaminationReportResponse complete(
            Long userId,
            Long examinationId,
            CompleteExaminationRequest request
    ) {
        ExaminationLockResponse examination =
                examinationMapper.selectOwnedExaminationForUpdate(
                        userId,
                        examinationId
                );
        if (examination == null) {
            throw new BusinessException(ErrorCode.EXAMINATION_NOT_FOUND);
        }
        if ("完成".equals(examination.status())) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }
        if (!"申请".equals(examination.status())
                && !"执行中".equals(examination.status())) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }

        ExaminationReport report = new ExaminationReport();
        report.setExaminationId(examinationId);
        report.setResult(request.getResult().trim());
        report.setRemark(normalizeOptionalText(request.getRemark()));
        try {
            examinationReportMapper.insert(report);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.EXAMINATION_REPORT_EXIST);
        }

        if (examinationMapper.markCompleted(examinationId) != 1) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }

        messageNotificationService.createExaminationCompleted(
                examination.patientUserId(),
                examinationId,
                report.getId()
        );

        return examinationMapper.selectDoctorOwnedReport(userId, examinationId);
    }

    /** 患者提交线下取得的检查结果，等待开单医生确认。 */
    @Transactional
    public PatientExaminationListItemResponse submitPatientResult(
            Long userId,
            Long examinationId,
            SubmitExaminationResultRequest request
    ) {
        ExaminationLockResponse examination =
                examinationMapper.selectPatientOwnedExaminationForUpdate(
                        userId,
                        examinationId
                );
        if (examination == null) {
            throw new BusinessException(ErrorCode.EXAMINATION_NOT_FOUND);
        }
        if (!STATUS_APPLIED.equals(examination.status())
                && !STATUS_NEEDS_REVISION.equals(examination.status())) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }

        ExaminationReport report =
                examinationReportMapper.selectByExaminationId(examinationId);
        if (report == null) {
            report = new ExaminationReport();
            report.setExaminationId(examinationId);
            report.setResult(request.getResult().trim());
            report.setRemark(normalizeOptionalText(request.getRemark()));
            examinationReportMapper.insert(report);
        } else {
            report.setResult(request.getResult().trim());
            report.setRemark(normalizeOptionalText(request.getRemark()));
            examinationReportMapper.updateById(report);
        }

        if (examinationMapper.markAwaitingDoctorReview(examinationId) != 1) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }

        return findPatientExamination(userId, examinationId);
    }

    /** 医生确认患者提交的检查结果，并将草稿转为正式报告。 */
    @Transactional
    public ExaminationReportResponse confirmPatientResult(
            Long userId,
            Long examinationId
    ) {
        ExaminationLockResponse examination =
                examinationMapper.selectOwnedExaminationForUpdate(userId, examinationId);
        if (examination == null) {
            throw new BusinessException(ErrorCode.EXAMINATION_NOT_FOUND);
        }
        if (!STATUS_AWAITING_REVIEW.equals(examination.status())
                || examinationReportMapper.selectByExaminationId(examinationId) == null) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }
        if (examinationMapper.markCompletedAfterReview(examinationId) != 1) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }

        ExaminationReport report = examinationReportMapper.selectByExaminationId(examinationId);
        messageNotificationService.createExaminationCompleted(
                examination.patientUserId(), examinationId, report.getId()
        );
        return examinationMapper.selectDoctorOwnedReport(userId, examinationId);
    }

    /** 医生退回检查结果，患者可按原因修改后再次提交。 */
    @Transactional
    public void rejectPatientResult(
            Long userId,
            Long examinationId,
            RejectExaminationRequest request
    ) {
        ExaminationLockResponse examination =
                examinationMapper.selectOwnedExaminationForUpdate(userId, examinationId);
        if (examination == null) {
            throw new BusinessException(ErrorCode.EXAMINATION_NOT_FOUND);
        }
        if (!STATUS_AWAITING_REVIEW.equals(examination.status())) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }
        ExaminationReport report = examinationReportMapper.selectByExaminationId(examinationId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        report.setRemark(request.getReason().trim());
        examinationReportMapper.updateById(report);
        if (examinationMapper.markNeedsRevision(examinationId) != 1) {
            throw new BusinessException(ErrorCode.EXAMINATION_STATUS_ERROR);
        }
        messageNotificationService.createExaminationNeedsRevision(
                examination.patientUserId(), examinationId, request.getReason().trim()
        );
    }

    /**
     * 根据登录角色执行不同的归属查询。
     * 没有报告、报告不属于当前用户，都返回相同的 404，避免泄露医疗隐私。
     */
    public ExaminationReportResponse getReport(
            Long examinationId,
            CurrentUserResponse currentUser
    ) {
        ExaminationReportResponse report;

        if ("patient".equals(currentUser.getRole())) {
            report = examinationMapper.selectPatientOwnedReport(
                    currentUser.getUserId(),
                    examinationId
            );
        } else if ("doctor".equals(currentUser.getRole())) {
            report = examinationMapper.selectDoctorOwnedReport(
                    currentUser.getUserId(),
                    examinationId
            );
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }

        return report;
    }

    
    private void normalizeStatus(DoctorExaminationQuery query) {
        if (query.getStatus() != null) {
            String status = query.getStatus().trim();
            query.setStatus(status.isEmpty() ? null : status);
        }
    }

    private PatientExaminationListItemResponse findPatientExamination(
            Long userId,
            Long examinationId
    ) {
        return examinationMapper.selectPatientExaminations(userId)
                .stream()
                .filter(item -> examinationId.equals(item.examinationId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAMINATION_NOT_FOUND));
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
