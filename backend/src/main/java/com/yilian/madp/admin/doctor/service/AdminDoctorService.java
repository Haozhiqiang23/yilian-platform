package com.yilian.madp.admin.doctor.service;

import com.yilian.madp.admin.doctor.dto.AdminDoctorListQuery;
import com.yilian.madp.admin.doctor.dto.DoctorAuditListQuery;
import com.yilian.madp.admin.doctor.dto.DoctorAuditRequest;
import com.yilian.madp.admin.doctor.dto.DoctorHistoryQuery;
import com.yilian.madp.admin.doctor.dto.UpdateDoctorStatusRequest;
import com.yilian.madp.admin.doctor.mapper.AdminDoctorMapper;
import com.yilian.madp.admin.doctor.vo.AdminDoctorListItemResponse;
import com.yilian.madp.admin.doctor.vo.AdminDoctorDetailResponse;
import com.yilian.madp.admin.doctor.vo.DoctorAuditLockResponse;
import com.yilian.madp.admin.doctor.vo.DoctorAuditResponse;
import com.yilian.madp.admin.doctor.vo.DoctorStatusResponse;
import com.yilian.madp.admin.doctor.vo.DoctorHistoryItemResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.yilian.madp.doctor.service.DoctorDetailCache;

/** 管理员医生管理业务层。 */
@Service
@RequiredArgsConstructor
public class AdminDoctorService {

    private final AdminDoctorMapper adminDoctorMapper;
    private final DoctorDetailCache doctorDetailCache;

    public PageResult<AdminDoctorListItemResponse> getDoctors(
            AdminDoctorListQuery query
    ) {
        query.setKeyword(trimToNull(query.getKeyword()));
        query.setStatus(trimToNull(query.getStatus()));
        int total = adminDoctorMapper.countDoctors(query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminDoctorListItemResponse> records =
                adminDoctorMapper.selectDoctors(query, offset, query.getSize());
        return new PageResult<>(records, total);
    }

    /** 管理员可以查看任意状态的医生资料。 */
    public AdminDoctorDetailResponse getDoctorDetail(Long doctorId) {
        AdminDoctorDetailResponse doctor =
                adminDoctorMapper.selectDoctorDetail(doctorId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }
        return doctor;
    }

    /** 查询指定医生的审核、禁用和恢复记录，按最新操作优先返回。 */
    public PageResult<DoctorHistoryItemResponse> getDoctorHistory(
            Long doctorId,
            DoctorHistoryQuery query
    ) {
        if (adminDoctorMapper.selectDoctorDetail(doctorId) == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }
        int total = adminDoctorMapper.countDoctorHistory(doctorId);
        int offset = (query.getPage() - 1) * query.getSize();
        List<DoctorHistoryItemResponse> records =
                adminDoctorMapper.selectDoctorHistory(
                        doctorId, offset, query.getSize()
                );
        return new PageResult<>(records, total);
    }

    public PageResult<AdminDoctorListItemResponse> getPendingAudits(
            DoctorAuditListQuery query
    ) {
        int total = adminDoctorMapper.countPendingAudits();
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminDoctorListItemResponse> records =
                adminDoctorMapper.selectPendingAudits(offset, query.getSize());
        return new PageResult<>(records, total);
    }

    /**
     * 医生状态、登录账号状态和操作日志必须在同一事务内更新。
     * 任意一步失败时全部回滚，避免出现审核通过但账号仍被禁用的情况。
     */
    @Transactional
    public DoctorAuditResponse auditDoctor(
            Long adminId,
            Long doctorId,
            DoctorAuditRequest request
    ) {
        DoctorAuditLockResponse doctor =
                adminDoctorMapper.selectDoctorForAudit(doctorId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }
        if (!"待审核".equals(doctor.doctorStatus())) {
            throw new BusinessException(ErrorCode.DOCTOR_AUDIT_STATUS_ERROR);
        }

        boolean approved = "PASS".equals(request.getResult())
                || "APPROVE".equals(request.getResult());
        String result = approved ? "PASS" : "REJECT";
        String doctorStatus = approved ? "启用" : "停用";
        String userStatus = approved ? "正常" : "禁用";

        if (adminDoctorMapper.updateDoctorAuditStatus(
                doctorId,
                doctorStatus,
                adminId,
                trimToNull(request.getReason())
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (adminDoctorMapper.updateDoctorUserStatus(
                doctor.userId(), userStatus
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        adminDoctorMapper.insertAuditLog(
                adminId,
                doctorId,
                doctor.name(),
                result,
                trimToNull(request.getReason())
        );
        // 【Redis查询优化】审核可能将空值变为可见详情，提交后删除对应缓存。
        doctorDetailCache.evictAfterCommit(doctorId);
        return new DoctorAuditResponse(
                doctorId, result, doctorStatus,
                approved ? "NORMAL" : "DISABLED"
        );
    }

    /**
     * 同步修改医生业务状态和登录账号状态，并记录管理员操作。
     * 三项操作位于同一事务，防止只禁用了登录账号却仍能被患者查询到。
     */
    @Transactional
    public DoctorStatusResponse updateDoctorStatus(
            Long adminId,
            Long doctorId,
            UpdateDoctorStatusRequest request
    ) {
        DoctorAuditLockResponse doctor =
                adminDoctorMapper.selectDoctorForAudit(doctorId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }

        boolean disabled = "DISABLED".equals(request.getStatus());
        String doctorStatus = disabled ? "停用" : "启用";
        String accountStatus = disabled ? "禁用" : "正常";
        if (doctorStatus.equals(doctor.doctorStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        if (adminDoctorMapper.updateDoctorStatus(
                doctorId,
                doctorStatus,
                adminId,
                trimToNull(request.getReason())
        ) != 1
                || adminDoctorMapper.updateDoctorUserStatus(
                doctor.userId(), accountStatus
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        adminDoctorMapper.insertStatusLog(
                adminId,
                doctorId,
                doctor.name(),
                disabled ? "禁用" : "恢复",
                trimToNull(request.getReason())
        );
        // 【Redis查询优化】禁用与恢复均失效，避免继续展示旧医生状态。
        doctorDetailCache.evictAfterCommit(doctorId);
        return new DoctorStatusResponse(
                doctorId, doctorStatus, request.getStatus()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
