package com.yilian.madp.admin.doctor.mapper;

import com.yilian.madp.admin.doctor.dto.AdminDoctorListQuery;
import com.yilian.madp.admin.doctor.vo.AdminDoctorListItemResponse;
import com.yilian.madp.admin.doctor.vo.AdminDoctorDetailResponse;
import com.yilian.madp.admin.doctor.vo.DoctorAuditLockResponse;
import com.yilian.madp.admin.doctor.vo.DoctorHistoryItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理员医生查询数据访问层。 */
@Mapper
public interface AdminDoctorMapper {

    int countDoctors(@Param("query") AdminDoctorListQuery query);

    List<AdminDoctorListItemResponse> selectDoctors(
            @Param("query") AdminDoctorListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    AdminDoctorDetailResponse selectDoctorDetail(
            @Param("doctorId") Long doctorId
    );

    int countDoctorHistory(@Param("doctorId") Long doctorId);

    List<DoctorHistoryItemResponse> selectDoctorHistory(
            @Param("doctorId") Long doctorId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countPendingAudits();

    List<AdminDoctorListItemResponse> selectPendingAudits(
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 锁定医生记录，避免两个管理员同时审核。 */
    DoctorAuditLockResponse selectDoctorForAudit(
            @Param("doctorId") Long doctorId
    );

    int updateDoctorAuditStatus(
            @Param("doctorId") Long doctorId,
            @Param("doctorStatus") String doctorStatus,
            @Param("adminId") Long adminId,
            @Param("reason") String reason
    );

    int updateDoctorStatus(
            @Param("doctorId") Long doctorId,
            @Param("doctorStatus") String doctorStatus,
            @Param("adminId") Long adminId,
            @Param("reason") String reason
    );

    int updateDoctorUserStatus(
            @Param("userId") Long userId,
            @Param("userStatus") String userStatus
    );

    int insertAuditLog(
            @Param("adminId") Long adminId,
            @Param("doctorId") Long doctorId,
            @Param("doctorName") String doctorName,
            @Param("result") String result,
            @Param("reason") String reason
    );

    int insertStatusLog(
            @Param("adminId") Long adminId,
            @Param("doctorId") Long doctorId,
            @Param("doctorName") String doctorName,
            @Param("action") String action,
            @Param("reason") String reason
    );
}
