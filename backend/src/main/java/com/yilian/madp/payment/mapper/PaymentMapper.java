package com.yilian.madp.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.payment.entity.Payment;
import com.yilian.madp.payment.vo.AppointmentPaymentLockResponse;
import com.yilian.madp.payment.vo.PaymentLockResponse;
import com.yilian.madp.payment.vo.PaymentDetailResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 支付订单数据访问层。
 * 普通新增使用 MyBatis-Plus，带行锁的预约查询放在 XML 中。
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
    /** 【支付取消并发修复】先普通读取关联编号，再锁当前患者的预约，最后锁支付单。 */
    Long selectAppointmentIdByPaymentId(@Param("paymentId") Long paymentId);
    /** 【超时取消并发修复】内部清理先单表锁预约，再执行关联状态更新。 */
    Long lockAppointmentId(@Param("appointmentId") Long appointmentId);

    Long lockOwnedAppointmentId(@Param("patientId") Long patientId,
                                @Param("appointmentId") Long appointmentId);

    /**
     * 只查询属于当前患者的预约并加排他锁。
     */
    AppointmentPaymentLockResponse selectOwnedAppointmentForUpdate(
            @Param("patientId") Long patientId,
            @Param("appointmentId") Long appointmentId
    );

    /**
     * 联表校验支付单归属并加排他锁，保证重复支付请求串行执行。
     */
    PaymentLockResponse selectOwnedPaymentForUpdate(
            @Param("patientId") Long patientId,
            @Param("paymentId") Long paymentId
    );

    /** 只允许待支付订单变为支付成功，并记录支付时间。 */
    int markPaymentSuccess(@Param("paymentId") Long paymentId);

    /** 支付成功后同步更新关联预约状态。 */
    int markAppointmentPaid(@Param("appointmentId") Long appointmentId);

    /**
     * 查询当前患者自己的支付订单详情。
     */
    PaymentDetailResponse selectOwnedPaymentDetail(
            @Param("patientId") Long patientId,
            @Param("paymentId") Long paymentId
    );

    /** 按预约查询当前患者已有的支付订单，供重复进入支付页时复用。 */
    PaymentDetailResponse selectOwnedPaymentByAppointment(
            @Param("patientId") Long patientId,
            @Param("appointmentId") Long appointmentId
    );

    /** 取消指定的超时待支付预约、关闭支付单并原子返还号源。 */
    int expirePendingAppointment(@Param("appointmentId") Long appointmentId);

    /** 根据支付单编号取消对应的超时预约。 */
    int expirePendingAppointmentByPaymentId(@Param("paymentId") Long paymentId);

    /** 【回补修复】查询候选；执行取消时仍须重新判断状态，不能凭查询直接返还库存。 */
    java.util.List<Long> selectExpiredAppointmentIds();
}
