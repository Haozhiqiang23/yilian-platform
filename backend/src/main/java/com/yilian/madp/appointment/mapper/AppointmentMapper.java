package com.yilian.madp.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.appointment.entity.Appointment;
import com.yilian.madp.appointment.vo.SourceLockResponse;
import com.yilian.madp.appointment.vo.AppointmentListItemResponse;
import com.yilian.madp.appointment.vo.AppointmentDetailResponse;
import com.yilian.madp.appointment.vo.CancelPaymentLockResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预约数据访问层。
 * 创建预约涉及行锁和跨表时段检查，因此相关 SQL 放在 XML 中。
 */
@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 根据登录用户查找并锁定患者记录。
     * 同一患者的并发预约请求会在这里排队执行。
     */
    Long selectPatientIdForUpdate(@Param("userId") Long userId);
    /**
     * 锁定要预约的号源，防止多个请求同时扣减同一号源。
     */
    SourceLockResponse selectSourceForUpdate(
            @Param("sourceId") Long sourceId
    );
    /**
     * 检查患者在所选号源的同一日期、同一时间段是否已有有效预约。
     */
    int countActiveSameTimeAppointment(
            @Param("patientId") Long patientId,
            @Param("sourceId") Long sourceId
    );
    /**
     * 只有 remain_count 大于 0 时才执行扣减。
     */
    int decreaseSource(@Param("sourceId") Long sourceId);

    /** 统计当前患者符合状态条件的预约数量。 */
    int countMine(
            @Param("patientId") Long patientId,
            @Param("status") String status
    );

    /** 分页查询当前患者自己的预约。 */
    List<AppointmentListItemResponse> selectMine(
            @Param("patientId") Long patientId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 查询当前患者自己的单条预约详情。
     */
    AppointmentDetailResponse selectOwnedDetail(
            @Param("patientId") Long patientId,
            @Param("appointmentId") Long appointmentId
    );

    /** 查询当前患者的预约并加排他锁。 */
    Appointment selectOwnedAppointmentForUpdate(
            @Param("patientId") Long patientId,
            @Param("appointmentId") Long appointmentId
    );

    /** 查询号源对应的就诊开始时间，用于取消时间校验。 */
    java.time.LocalDateTime selectVisitStartTime(@Param("sourceId") Long sourceId);

    /** 将待支付预约改为取消。 */
    int cancelPendingAppointment(@Param("appointmentId") Long appointmentId);

    /** 取消未支付预约后恢复一个号源。 */
    int increaseSource(@Param("sourceId") Long sourceId);

    /** 如果预约已生成待支付订单，则将支付单关闭。 */
    int closePendingPayment(@Param("appointmentId") Long appointmentId);

    /** 查询并锁定已支付预约对应的支付单。 */
    CancelPaymentLockResponse selectPaymentForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    /** 已支付预约取消后进入退款中状态。 */
    int markAppointmentRefunding(
            @Param("appointmentId") Long appointmentId
    );
}
