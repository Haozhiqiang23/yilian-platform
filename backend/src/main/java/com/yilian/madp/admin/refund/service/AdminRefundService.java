package com.yilian.madp.admin.refund.service;

import com.yilian.madp.admin.refund.dto.AdminRefundListQuery;
import com.yilian.madp.admin.refund.dto.ProcessRefundRequest;
import com.yilian.madp.admin.refund.mapper.AdminRefundMapper;
import com.yilian.madp.admin.refund.vo.AdminRefundListItemResponse;
import com.yilian.madp.admin.refund.vo.RefundProcessLockResponse;
import com.yilian.madp.admin.refund.vo.RefundProcessResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.message.service.MessageNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 管理员退款查询与审核业务层。 */
@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final AdminRefundMapper adminRefundMapper;
    private final MessageNotificationService messageNotificationService;

    public PageResult<AdminRefundListItemResponse> getRefunds(
            AdminRefundListQuery query
    ) {
        query.setStatus(trimToNull(query.getStatus()));
        query.setKeyword(trimToNull(query.getKeyword()));
        int total = adminRefundMapper.countRefunds(query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminRefundListItemResponse> records =
                adminRefundMapper.selectRefunds(
                        query, offset, query.getSize()
                );
        return new PageResult<>(records, total);
    }

    /**
     * 确认退款成功、关闭预约、返还号源、通知患者和记录日志必须同时成功。
     */
    @Transactional
    public RefundProcessResponse processRefund(
            Long adminId,
            Long refundId,
            ProcessRefundRequest request
    ) {
//        退款订单
        RefundProcessLockResponse refund =
                adminRefundMapper.selectRefundForUpdate(refundId);
        if (refund == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
//退款状态
        boolean refundingAppointment =
                "退款中".equals(refund.appointmentStatus());
        boolean legacyCancelledAppointment =
                "取消".equals(refund.appointmentStatus());
//        不是退款申请或者  不是退款中  或者不是取消  抛异常
        if (!"退款申请".equals(refund.status())
                || (!refundingAppointment && !legacyCancelledAppointment)) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR);
        }

        String note = trimToNull(request.getNote());
        boolean approve = "退款成功".equals(request.getStatus());

        if (!approve && note == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
//        是退款成功且原因不是null且医生停诊抛异常
        if (!approve && refund.reason() != null
                && refund.reason().startsWith("医生停诊：")) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR);
        }
//        修改状态   修改没成功抛异常
        if (adminRefundMapper.updateRefundResult(
                refundId, request.getStatus(), note
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        // 新流程在退款成功时取消预约并回补号源；拒绝后恢复为已支付。
        // 兼容旧种子数据：预约已取消说明号源已经回补，不能再次增加。
        if (approve && refundingAppointment
                && (adminRefundMapper.cancelRefundingAppointment(   //将状态为退款中设置为取消
                refund.appointmentId()
        ) != 1 || adminRefundMapper.restoreSource(refund.sourceId()) != 1)) {   //恢复号源
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (!approve && (!refundingAppointment
                || adminRefundMapper.restoreAppointmentPaid(
                refund.appointmentId()
        ) != 1)) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR);
        }

        messageNotificationService.createRefundProcessed(
                refund.recipientUserId(),
                refund.appointmentId(),
                refundId,
                approve,
                note
        );
        adminRefundMapper.insertProcessLog(
                adminId, refundId, refund.appointmentId(),
                approve ? "APPROVE" : "REJECT", note
        );

        return new RefundProcessResponse(
                refundId, refund.appointmentId(), request.getStatus()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
