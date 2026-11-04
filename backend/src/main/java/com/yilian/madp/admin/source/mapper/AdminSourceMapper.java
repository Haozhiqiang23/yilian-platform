package com.yilian.madp.admin.source.mapper;

import com.yilian.madp.admin.source.dto.AdminSourceListQuery;
import com.yilian.madp.admin.source.vo.AdminSourceListItemResponse;
import com.yilian.madp.admin.source.vo.AdminSourceOverviewResponse;
import com.yilian.madp.admin.source.vo.AdminSourceLockResponse;
import com.yilian.madp.admin.source.vo.AdminSourceUpdateResponse;
import com.yilian.madp.admin.source.vo.SourceSuspensionPreviewResponse;
import com.yilian.madp.admin.source.vo.SourceAffectedAppointmentResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;

import java.util.List;

/** 管理员号源查询数据访问层。 */
@Mapper
public interface AdminSourceMapper {

    String selectActiveDoctorTitle(@Param("doctorId") Long doctorId);

    int countNewSourceConflict(
            @Param("doctorId") Long doctorId,
            @Param("visitDate") java.time.LocalDate visitDate,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    int insertSource(@Param("doctorId") Long doctorId, @Param("visitDate") java.time.LocalDate visitDate,
                     @Param("timePeriod") String timePeriod, @Param("startTime") java.time.LocalTime startTime,
                     @Param("endTime") java.time.LocalTime endTime, @Param("fee") BigDecimal fee,
                     @Param("totalCount") Integer totalCount);

    Long selectLastInsertId();

    int insertSourceCreateLog(
            @Param("adminId") Long adminId,
            @Param("sourceId") Long sourceId,
            @Param("target") String target
    );

    int countSources(@Param("query") AdminSourceListQuery query);

    /** 查询全部符合筛选条件的号源概览，不受分页影响。 */
    AdminSourceOverviewResponse selectOverview(
            @Param("query") AdminSourceListQuery query
    );

    List<AdminSourceListItemResponse> selectSources(
            @Param("query") AdminSourceListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    AdminSourceLockResponse selectSourceForUpdate(
            @Param("sourceId") Long sourceId
    );

    int countScheduleConflict(
            @Param("sourceId") Long sourceId,
            @Param("doctorId") Long doctorId,
            @Param("visitDate") java.time.LocalDate visitDate,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    int updateSource(
            @Param("sourceId") Long sourceId,
            @Param("visitDate") java.time.LocalDate visitDate,
            @Param("timePeriod") String timePeriod,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("totalCount") Integer totalCount,
            @Param("remainCount") Integer remainCount
    );

    int insertSourceAdjustLog(
            @Param("adminId") Long adminId,
            @Param("sourceId") Long sourceId,
            @Param("target") String target,
            @Param("reason") String reason
    );

    SourceSuspensionPreviewResponse selectSuspensionPreview(
            @Param("sourceId") Long sourceId
    );

    int countActiveAppointments(@Param("sourceId") Long sourceId);

    List<SourceAffectedAppointmentResponse> selectActiveAppointmentsForUpdate(
            @Param("sourceId") Long sourceId
    );

    int cancelPendingAppointments(@Param("sourceId") Long sourceId);

    int markPaidAppointmentsRefunding(@Param("sourceId") Long sourceId);

    int closePendingPayments(@Param("sourceId") Long sourceId);

    int restoreCancelledSourceCount(
            @Param("sourceId") Long sourceId,
            @Param("cancelledCount") Integer cancelledCount
    );

    int updateSourceStatus(
            @Param("sourceId") Long sourceId,
            @Param("status") String status,
            @Param("reason") String reason,
            @Param("adminId") Long adminId
    );

    int insertSourceStatusLog(
            @Param("adminId") Long adminId,
            @Param("sourceId") Long sourceId,
            @Param("action") String action,
            @Param("target") String target,
            @Param("reason") String reason
    );

}
