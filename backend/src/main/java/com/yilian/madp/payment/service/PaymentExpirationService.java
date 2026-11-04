package com.yilian.madp.payment.service;

import com.yilian.madp.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** 请求触发超时清理，同时取消预约并返还号源；当前没有定时调度入口。 */
@Service
@RequiredArgsConstructor
public class PaymentExpirationService {

    private final PaymentMapper paymentMapper;

    // 【并发修复：分离清理事务】不能让全局清理的预约表锁一直带入创建流程。
    // 否则可能形成“清理持有预约锁等待号源，创建持有号源锁等待插入”的死锁。
    // 由其他Service调用本Bean，REQUIRES_NEW经Spring代理生效；清理完成先提交，
    // 再恢复外层创建事务。新预约失败不撤销已经完成的历史过期清理。
    // 这不替代后续对清理SQL回补数量及创建/取消混合场景的单独验证。
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirePendingAppointments() {
        // 【回补修复】同一号源可能有多条过期预约，必须按预约逐条返还。
        // 每条更新仍校验待支付和超时；重复清理已取消预约时不会重复加库存。
        // 本方法事务内同时完成取消、关闭支付单和号源回补，失败共同回滚。
        for (Long appointmentId : paymentMapper.selectExpiredAppointmentIds()) {
            expireLockedAppointment(appointmentId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireAppointment(Long appointmentId) {
        expireLockedAppointment(appointmentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirePayment(Long paymentId) {
        // 【超时取消并发修复】原按支付ID的联表UPDATE可能先锁支付单，
        // 即使未超时也会与取消形成反向持锁。先普通查关联ID，再显式锁预约。
        Long appointmentId = paymentMapper.selectAppointmentIdByPaymentId(paymentId);
        if (appointmentId != null) {
            expireLockedAppointment(appointmentId);
        }
    }

    private void expireLockedAppointment(Long appointmentId) {
        // 当前事务已由外部调用的public方法建立；私有方法不再开启事务。
        // 先锁预约后，更新仍重新判断待支付及超时条件，避免重复取消和回补。
        if (paymentMapper.lockAppointmentId(appointmentId) != null) {
            paymentMapper.expirePendingAppointment(appointmentId);
        }
    }
}
