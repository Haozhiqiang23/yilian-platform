package com.yilian.madp.admin.refund.mapper;

import com.yilian.madp.admin.refund.dto.AdminRefundListQuery;
import com.yilian.madp.admin.refund.vo.AdminRefundListItemResponse;
import com.yilian.madp.admin.refund.vo.RefundProcessLockResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理员退款管理数据访问层。 */
@Mapper
public interface AdminRefundMapper {

    int countRefunds(@Param("query") AdminRefundListQuery query);

    List<AdminRefundListItemResponse> selectRefunds(
            @Param("query") AdminRefundListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    RefundProcessLockResponse selectRefundForUpdate(@Param("id") Long id);

    int updateRefundResult(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("note") String note
    );

    int cancelRefundingAppointment(@Param("appointmentId") Long appointmentId);

    int restoreSource(@Param("sourceId") Long sourceId);

    int restoreAppointmentPaid(@Param("appointmentId") Long appointmentId);

    int insertProcessLog(
            @Param("adminId") Long adminId,
            @Param("refundId") Long refundId,
            @Param("appointmentId") Long appointmentId,
            @Param("action") String action,
            @Param("note") String note
    );
}
