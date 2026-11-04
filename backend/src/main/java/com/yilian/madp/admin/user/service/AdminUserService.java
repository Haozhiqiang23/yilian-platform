package com.yilian.madp.admin.user.service;

import com.yilian.madp.admin.doctor.dto.UpdateDoctorStatusRequest;
import com.yilian.madp.admin.doctor.service.AdminDoctorService;
import com.yilian.madp.admin.user.dto.AdminUserListQuery;
import com.yilian.madp.admin.user.dto.UpdateUserStatusRequest;
import com.yilian.madp.admin.user.mapper.AdminUserMapper;
import com.yilian.madp.admin.user.vo.AdminUserListItemResponse;
import com.yilian.madp.admin.user.vo.AdminUserStatusResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 管理员用户管理业务层。 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final AdminDoctorService adminDoctorService;

    /** 查询用户列表；先统一清理文本条件，避免空格影响匹配。 */
    public PageResult<AdminUserListItemResponse> getUsers(
            AdminUserListQuery query
    ) {
        query.setKeyword(trimToNull(query.getKeyword()));
        query.setRole(trimToNull(query.getRole()));
        query.setStatus(trimToNull(query.getStatus()));

        int total = adminUserMapper.countUsers(query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminUserListItemResponse> records =
                adminUserMapper.selectUsers(query, offset, query.getSize());
        return new PageResult<>(records, total);
    }

    /**
     * 修改账号状态。当前管理员 ID 来自 JWT，不能由前端伪造。
     */
    @Transactional
    public AdminUserStatusResponse updateStatus(
            Long currentAdminId,
            Long userId,
            UpdateUserStatusRequest request
    ) {
        if (currentAdminId.equals(userId)
                && "DISABLED".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
        }

        String role = adminUserMapper.selectRoleById(userId);
        if (role == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 用户管理中的医生账号复用医生管理的完整状态链路：医生业务状态、
        // 登录账号状态与管理员操作日志在同一事务中更新。
        if ("DOCTOR".equalsIgnoreCase(role)) {
            Long doctorId = adminUserMapper.selectDoctorIdByUserId(userId);
            if (doctorId == null) {
                throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
            }
            UpdateDoctorStatusRequest doctorRequest =
                    new UpdateDoctorStatusRequest();
            doctorRequest.setStatus(request.getStatus());
            doctorRequest.setReason(trimToNull(request.getReason()));
            adminDoctorService.updateDoctorStatus(
                    currentAdminId, doctorId, doctorRequest
            );
            return new AdminUserStatusResponse(userId, request.getStatus());
        }

        if (adminUserMapper.updateStatus(userId, request.getStatus()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        adminUserMapper.insertStatusLog(
                currentAdminId,
                userId,
                "DISABLED".equals(request.getStatus()) ? "禁用" : "恢复",
                trimToNull(request.getReason())
        );

        return new AdminUserStatusResponse(userId, request.getStatus());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
