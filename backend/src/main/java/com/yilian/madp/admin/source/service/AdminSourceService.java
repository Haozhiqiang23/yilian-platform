package com.yilian.madp.admin.source.service;

import com.yilian.madp.admin.source.dto.AdminSourceListQuery;
import com.yilian.madp.admin.source.dto.UpdateAdminSourceRequest;
import com.yilian.madp.admin.source.dto.CreateAdminSourceRequest;
import com.yilian.madp.admin.source.dto.UpdateSourceStatusRequest;
import com.yilian.madp.admin.source.mapper.AdminSourceMapper;
import com.yilian.madp.admin.source.vo.AdminSourceListItemResponse;
import com.yilian.madp.admin.source.vo.AdminSourceOverviewResponse;
import com.yilian.madp.admin.source.vo.AdminSourcePageResponse;
import com.yilian.madp.admin.source.vo.AdminSourceLockResponse;
import com.yilian.madp.admin.source.vo.AdminSourceUpdateResponse;
import com.yilian.madp.admin.source.vo.AdminSourceCreateResponse;
import com.yilian.madp.admin.source.vo.SourceSuspensionPreviewResponse;
import com.yilian.madp.admin.source.vo.SourceStatusResponse;
import com.yilian.madp.admin.source.vo.SourceAffectedAppointmentResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.message.service.MessageNotificationService;
import com.yilian.madp.refund.entity.Refund;
import com.yilian.madp.refund.mapper.RefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * 管理员号源管理业务层。
 *
 * 主要负责：号源分页查询、排班调整、停诊影响预览、停诊与恢复。
 * Controller 只接收请求，具体业务校验和事务控制统一放在这里。
 */
@Service
@RequiredArgsConstructor
public class AdminSourceService {

    /** 查询和修改号源、预约及管理员操作日志。 */
    private final AdminSourceMapper adminSourceMapper;

    /** 给受停诊影响的患者创建站内消息。 */
    private final MessageNotificationService messageNotificationService;

    /** 为已支付预约创建退款申请。 */
    private final RefundMapper refundMapper;

    /** 校验医生和排班时间后创建号源，并记录管理员操作日志。 */
    @Transactional
    public AdminSourceCreateResponse createSource(
            Long adminId,
            CreateAdminSourceRequest request
    ) {
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.SOURCE_TIME_INVALID);
        }
        if (request.getVisitDate().equals(LocalDate.now())
                && !endTime.isAfter(LocalTime.now())) {
            throw new BusinessException(ErrorCode.SOURCE_CREATE_EXPIRED);
        }
        String doctorTitle = adminSourceMapper.selectActiveDoctorTitle(
                request.getDoctorId()
        );
        if (doctorTitle == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }
        if (adminSourceMapper.countNewSourceConflict(
                request.getDoctorId(), request.getVisitDate(), startTime, endTime
        ) > 0) {
            throw new BusinessException(ErrorCode.SOURCE_TIME_CONFLICT);
        }

        String timePeriod = startTime + "-" + endTime;
        BigDecimal fee = registrationFeeOf(doctorTitle);
        if (adminSourceMapper.insertSource(
                request.getDoctorId(), request.getVisitDate(), timePeriod,
                startTime, endTime, fee, request.getTotalCount()
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Long sourceId = adminSourceMapper.selectLastInsertId();
        String target = "号源ID=" + sourceId
                + "，医生ID=" + request.getDoctorId()
                + "，排班=" + request.getVisitDate() + " " + timePeriod
                + "，总号数=" + request.getTotalCount()
                + "，挂号费=" + fee;
        adminSourceMapper.insertSourceCreateLog(adminId, sourceId, target);

        return new AdminSourceCreateResponse(
                sourceId, request.getDoctorId(), request.getVisitDate(),
                startTime, endTime, request.getTotalCount(), fee
        );
    }

    /** 挂号费由医生职称统一确定，避免前端金额和支付订单不一致。 */
    private BigDecimal registrationFeeOf(String doctorTitle) {
        if ("主任医师".equals(doctorTitle)) {
            return new BigDecimal("50.00");
        }
        if ("副主任医师".equals(doctorTitle)) {
            return new BigDecimal("30.00");
        }
        return new BigDecimal("20.00");
    }

    /** 统计和分页查询必须复用同一组筛选条件，保证总数与列表一致。 */
    public AdminSourcePageResponse getSources(
            AdminSourceListQuery query
    ) {
        query.setDoctorName(trimToNull(query.getDoctorName()));
        query.setStatus(trimToNull(query.getStatus()));
        // total 用于前端分页；overview 是全部筛选结果的汇总，不受分页影响。
        int total = adminSourceMapper.countSources(query);
        AdminSourceOverviewResponse overview =
                adminSourceMapper.selectOverview(query);
        // MySQL LIMIT 的偏移量，例如第 3 页、每页 10 条，offset 为 20。
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminSourceListItemResponse> records =
                adminSourceMapper.selectSources(query, offset, query.getSize());
        return new AdminSourcePageResponse(records, total, overview);
    }

    /**
     * 调整号源时先锁定记录，避免与患者预约并发修改剩余数量。
     * 已有预约时只允许调整总号数，不能直接改变患者已经预约的出诊时间。
     */
    @Transactional
    public AdminSourceUpdateResponse updateSource(
            Long adminId,
            Long sourceId,
            UpdateAdminSourceRequest request
    ) {
        // SELECT ... FOR UPDATE 锁定号源，防止调整期间患者并发预约。
        AdminSourceLockResponse source =
                adminSourceMapper.selectSourceForUpdate(sourceId);
        if (source == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND);
        }
        if (source.visitDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.SOURCE_EXPIRED);
        }

        // 请求没有传的字段继续使用数据库原值，支持只修改其中一项。
        LocalDate newVisitDate = request.getVisitDate() == null
                ? source.visitDate() : request.getVisitDate();
        String requestedPeriod = trimToNull(request.getTimePeriod());
        String newTimePeriod = requestedPeriod == null
                ? source.timePeriod() : requestedPeriod;
        LocalTime newStartTime = request.getStartTime() == null
                ? source.startTime() : request.getStartTime();
        LocalTime newEndTime = request.getEndTime() == null
                ? source.endTime() : request.getEndTime();
        if (newStartTime == null || newEndTime == null
                || !newStartTime.isBefore(newEndTime)) {
            throw new BusinessException(ErrorCode.SOURCE_TIME_INVALID);
        }
        int newTotalCount = request.getTotalCount() == null
                ? source.totalCount() : request.getTotalCount();

        // 已预约数不会因为管理员调整总号数而变化。
        // 以预约表为准，避免号源剩余数因历史数据或并发操作与真实预约数不一致。
        int bookedCount = source.bookedCount() == null ? 0 : source.bookedCount();
        if (newTotalCount < bookedCount) {
            throw new BusinessException(ErrorCode.SOURCE_TOTAL_TOO_SMALL);
        }

        // 日期或时段任意一个变化，都属于排班时间发生改变。
        boolean scheduleChanged = !newVisitDate.equals(source.visitDate())
                || !Objects.equals(newStartTime, source.startTime())
                || !Objects.equals(newEndTime, source.endTime());
        String reason = trimToNull(request.getReason());
        if (scheduleChanged && bookedCount > 0) {
            throw new BusinessException(ErrorCode.SOURCE_HAS_APPOINTMENTS);
        }
        if (scheduleChanged && reason == null) {
            throw new BusinessException(
                    ErrorCode.SOURCE_ADJUST_REASON_REQUIRED
            );
        }
        if (scheduleChanged
                && adminSourceMapper.countScheduleConflict(
                sourceId, source.doctorId(), newVisitDate, newStartTime, newEndTime
        ) > 0) {
            throw new BusinessException(ErrorCode.SOURCE_TIME_CONFLICT);
        }

        // 新剩余数 = 新总号数 - 原有已预约数，避免覆盖患者已经占用的号源。
        int newRemainCount = newTotalCount - bookedCount;
        if (adminSourceMapper.updateSource(
                sourceId, newVisitDate, newTimePeriod, newStartTime, newEndTime,
                newTotalCount, newRemainCount
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        // 保存调整前后数据，便于后续审计和问题追踪。
        String target = "号源ID=" + sourceId
                + "，医生=" + source.doctorName()
                + "，原排班=" + source.visitDate() + " " + source.timePeriod()
                + "，新排班=" + newVisitDate + " " + newTimePeriod
                + "，原总号数=" + source.totalCount()
                + "，新总号数=" + newTotalCount;
        adminSourceMapper.insertSourceAdjustLog(
                adminId, sourceId, target, reason
        );

        return new AdminSourceUpdateResponse(
                sourceId, newVisitDate, newTimePeriod,
                newStartTime, newEndTime,
                newTotalCount, newRemainCount
        );
    }

    /** 查询停诊会影响的预约数量，不产生任何数据修改。 */
    public SourceSuspensionPreviewResponse getSuspensionPreview(Long sourceId) {
        SourceSuspensionPreviewResponse preview =
                adminSourceMapper.selectSuspensionPreview(sourceId);
        if (preview == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND);
        }
        return preview;
    }

    /**
     * 停诊时分类处理有效预约：
     * 待支付预约直接取消，已支付预约进入退款流程；
     * 已签到、接诊中或退款中的预约需要人工处理。
     * 所有预约、退款、消息、号源状态和操作日志都位于同一事务。
     */
    @Transactional
    public SourceStatusResponse updateSourceStatus(
            Long adminId,
            Long sourceId,
            UpdateSourceStatusRequest request
    ) {
        // 先锁号源，避免停诊过程中产生新的预约。
        AdminSourceLockResponse source =
                adminSourceMapper.selectSourceForUpdate(sourceId);

        // 不存在和已过期的号源都不能继续操作。
        if (source == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND);
        }
        if (source.visitDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.SOURCE_EXPIRED);
        }
        // 接口使用英文状态，数据库保存中文状态。
        boolean suspend = "SUSPENDED".equals(request.getStatus());
        String targetStatus = suspend ? "停诊" : "正常";
        String reason = trimToNull(request.getReason());
        if (targetStatus.equals(source.status())) {
            throw new BusinessException(ErrorCode.SOURCE_STATUS_ERROR);
        }
        if (suspend && reason == null) {
            throw new BusinessException(
                    ErrorCode.SOURCE_SUSPENSION_REASON_REQUIRED
            );
        }
        // 恢复号源不处理预约，因此默认处理数量为 0。
        int processedAppointmentCount = 0;
        if (suspend) {
            // 锁定全部有效预约，防止处理期间患者支付、取消或签到。
            List<SourceAffectedAppointmentResponse> appointments =
                    adminSourceMapper.selectActiveAppointmentsForUpdate(sourceId);

            // 只有待支付和已支付可以自动处理，其他状态必须人工介入。
            boolean hasUnsupportedStatus = appointments.stream()
                    .anyMatch(item -> !"待支付".equals(item.status())
                            && !"已支付".equals(item.status()));
            if (hasUnsupportedStatus) {
                throw new BusinessException(
                        ErrorCode.SOURCE_ACTIVE_APPOINTMENTS_EXIST
                );
            }

            // 将预约按原状态分组，后续分别执行取消和退款逻辑。
            List<SourceAffectedAppointmentResponse> pendingAppointments =
                    appointments.stream()
                            .filter(item -> "待支付".equals(item.status()))
                            .toList();
            List<SourceAffectedAppointmentResponse> paidAppointments =
                    appointments.stream()
                            .filter(item -> "已支付".equals(item.status()))
                            .toList();

            // 已支付预约必须存在支付成功的订单，异常数据不能自动进入退款流程。
            boolean hasInvalidPayment = paidAppointments.stream()
                    .anyMatch(item -> item.paymentId() == null
                            || item.paymentAmount() == null
                            || !"支付成功".equals(item.paymentStatus()));
            if (hasInvalidPayment) {
                throw new BusinessException(ErrorCode.PAYMENT_STATUS_ERROR);
            }

            // 返回给前端，用于提示本次停诊实际影响了多少条预约。
            processedAppointmentCount = appointments.size();
            if (processedAppointmentCount > 0) {
                // 待支付预约直接取消，并关闭可能已经创建的待支付订单。
                int cancelled =
                        adminSourceMapper.cancelPendingAppointments(sourceId);
                if (cancelled != pendingAppointments.size()) {
                    throw new BusinessException(ErrorCode.CONFLICT);
                }
                adminSourceMapper.closePendingPayments(sourceId);

                // 已支付预约不能直接取消，先改为退款中并生成退款申请。
                int refunding =
                        adminSourceMapper.markPaidAppointmentsRefunding(sourceId);
                if (refunding != paidAppointments.size()) {
                    throw new BusinessException(ErrorCode.CONFLICT);
                }
                // 每个支付单只能有一条退款记录，数据库唯一约束会防止重复退款。
                for (SourceAffectedAppointmentResponse appointment
                        : paidAppointments) {
                    Refund refund = new Refund();
                    refund.setPaymentId(appointment.paymentId());
                    refund.setAmount(appointment.paymentAmount());
                    refund.setStatus("退款申请");
                    refund.setReason("医生停诊：" + reason);
                    refundMapper.insert(refund);
                }

                // 此处只返还待支付预约占用的号源。
                // 已支付预约要等管理员确认退款成功后，再由退款业务返还号源。
                if (adminSourceMapper.restoreCancelledSourceCount(
                        sourceId, cancelled
                ) != 1) {
                    throw new BusinessException(ErrorCode.CONFLICT);
                }

                // 根据预约原状态发送不同消息，避免把“退款中”说成“退款成功”。
                String visitTime = source.visitDate() + " "
                        + source.timePeriod();
                for (SourceAffectedAppointmentResponse appointment
                        : appointments) {
                    if ("已支付".equals(appointment.status())) {
                        messageNotificationService.createSourceSuspensionRefunding(
                                appointment.recipientUserId(),
                                appointment.appointmentId(),
                                source.doctorName(),
                                visitTime,
                                reason
                        );
                    } else {
                        messageNotificationService.createSourceSuspensionCancelled(
                                appointment.recipientUserId(),
                                appointment.appointmentId(),
                                source.doctorName(),
                                visitTime,
                                reason
                        );
                    }
                }
            }
        }

        // 预约处理全部成功后，最后再修改号源状态。
        if (adminSourceMapper.updateSourceStatus(
                sourceId, targetStatus, reason, adminId
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        // 记录操作者、目标号源、排班及原因，形成审计链路。
        String action = suspend ? "SUSPEND" : "RESTORE";
        String target = "号源ID=" + sourceId
                + "，医生=" + source.doctorName()
                + "，排班=" + source.visitDate() + " " + source.timePeriod()
                + "，操作=" + (suspend ? "停诊" : "恢复");
        adminSourceMapper.insertSourceStatusLog(
                adminId, sourceId, action, target, reason
        );
        return new SourceStatusResponse(
                sourceId, targetStatus, reason, processedAppointmentCount
        );
    }

    /** 去掉首尾空格，并把空字符串统一转换成 null，简化后续判断。 */
    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
