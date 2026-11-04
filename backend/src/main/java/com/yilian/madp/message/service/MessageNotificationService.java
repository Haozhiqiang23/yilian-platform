package com.yilian.madp.message.service;

import com.yilian.madp.message.entity.Message;
import com.yilian.madp.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 业务通知写入服务。
 *
 * 各业务模块只需要说明“发生了什么”，不需要重复组装 message 表字段。
 * 本服务不单独开启事务，调用方已有事务时会自动加入同一个事务。
 */
@Service
@RequiredArgsConstructor
public class MessageNotificationService {

    private static final String TYPE_APPOINTMENT = "APPOINTMENT";
    private static final String TYPE_PAYMENT = "PAYMENT";
    private static final String TYPE_REMINDER = "REMINDER";
    private static final String RELATED_APPOINTMENT = "APPOINTMENT";
    private static final String RELATED_PAYMENT = "PAYMENT";
    private static final String RELATED_PRESCRIPTION = "PRESCRIPTION";
    private static final String RELATED_EXAMINATION = "EXAMINATION";

    private final MessageMapper messageMapper;

    /** 预约成功后向当前患者写入一条站内消息。 */
    public void createAppointmentSuccess(
            Long recipientUserId,
            Long appointmentId
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_APPOINTMENT);
        message.setTitle("预约成功");
        message.setContent(
                "您的预约已创建成功，预约编号为 " + appointmentId
                        + "，请及时完成支付。"
        );
        message.setRelatedType(RELATED_APPOINTMENT);
        message.setRelatedId(appointmentId);
        message.setIsRead(false);
        messageMapper.insert(message);

    }

    /** 支付首次成功后向付款患者写入一条站内消息。 */
    public void createPaymentSuccess(
            Long recipientUserId,
            Long paymentId,
            Long appointmentId
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_PAYMENT);
        message.setTitle("支付成功");
        message.setContent(
                "支付订单 " + paymentId + " 已支付成功，预约编号为 "
                        + appointmentId + "。"
        );
        message.setRelatedType(RELATED_PAYMENT);
        message.setRelatedId(paymentId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /**
     * 预约取消后写入通知。
     * 已支付预约并不是立即退款成功，因此使用“取消申请已提交”避免误导患者。
     */
    public void createAppointmentCancelled(
            Long recipientUserId,
            Long appointmentId,
            boolean refunding
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_APPOINTMENT);
        message.setTitle(refunding ? "取消申请已提交" : "预约已取消");
        message.setContent(
                refunding
                        ? "预约 " + appointmentId
                                + " 已取消并进入退款流程，请等待管理员处理。"
                        : "预约 " + appointmentId
                                + " 已成功取消，对应号源已恢复。"
        );
        message.setRelatedType(RELATED_APPOINTMENT);
        message.setRelatedId(appointmentId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 管理员停诊导致待支付预约被系统自动取消。 */
    public void createSourceSuspensionCancelled(
            Long recipientUserId,
            Long appointmentId,
            String doctorName,
            String visitTime,
            String reason
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_APPOINTMENT);
        message.setTitle("医生停诊通知");
        message.setContent(
                "由于" + doctorName + "在" + visitTime
                        + "停诊，您的待支付预约 " + appointmentId
                        + " 已自动取消。停诊原因：" + reason + "。"
        );
        message.setRelatedType(RELATED_APPOINTMENT);
        message.setRelatedId(appointmentId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 管理员停诊导致已支付预约进入退款流程。 */
    public void createSourceSuspensionRefunding(
            Long recipientUserId,
            Long appointmentId,
            String doctorName,
            String visitTime,
            String reason
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_APPOINTMENT);
        message.setTitle("医生停诊退款通知");
        message.setContent(
                "由于" + doctorName + "在" + visitTime
                        + "停诊，您的已支付预约 " + appointmentId
                        + " 已进入退款流程。停诊原因：" + reason + "。"
        );
        message.setRelatedType(RELATED_APPOINTMENT);
        message.setRelatedId(appointmentId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 管理员确认退款完成后通知患者。 */
    public void createRefundProcessed(
            Long recipientUserId,
            Long appointmentId,
            Long refundId,
            boolean approved,
            String note
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_PAYMENT);
        message.setTitle(approved ? "退款成功" : "退款申请已拒绝");
        message.setContent(
                approved
                        ? "预约 " + appointmentId + " 的退款申请 " + refundId
                                + " 已处理成功，请留意原支付渠道到账情况。"
                        : "预约 " + appointmentId + " 的退款申请 " + refundId
                                + " 未通过，预约已恢复。原因：" + note + "。"
        );
        message.setRelatedType(RELATED_APPOINTMENT);
        message.setRelatedId(appointmentId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 签到和排队记录创建完成后，告知患者本次排队号码。 */
    public void createSignInSuccess(
            Long recipientUserId,
            Long appointmentId,
            String queueNo
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_REMINDER);
        message.setTitle("签到成功");
        message.setContent(
                "预约 " + appointmentId + " 已签到成功，您的排队号为 "
                        + queueNo + "，请留意叫号。"
        );
        message.setRelatedType(RELATED_APPOINTMENT);
        message.setRelatedId(appointmentId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 医生完成接诊并生成诊疗记录后通知患者。 */
    public void createConsultationCompleted(
            Long recipientUserId,
            Long appointmentId,
            Long medicalRecordId
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_REMINDER);
        message.setTitle("诊疗已完成");
        message.setContent(
                "预约 " + appointmentId + " 已完成诊疗，诊疗记录编号为 "
                        + medicalRecordId + "，可在诊疗记录中查看详情。"
        );
        message.setRelatedType("MEDICAL_RECORD");
        message.setRelatedId(medicalRecordId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 医生开具处方后通知患者，并关联到处方详情。 */
    public void createPrescriptionGenerated(
            Long recipientUserId,
            Long prescriptionId,
            Long medicalRecordId
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_REMINDER);
        message.setTitle("处方已生成");
        message.setContent(
                "诊疗记录 " + medicalRecordId + " 已生成处方，处方编号为 "
                        + prescriptionId + "，可在诊疗记录中查看详情。"
        );
        message.setRelatedType(RELATED_PRESCRIPTION);
        message.setRelatedId(prescriptionId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 医生创建检查申请后通知患者。 */
    public void createExaminationRequested(
            Long recipientUserId,
            Long examinationId,
            String itemName
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_REMINDER);
        message.setTitle("检查申请已生成");
        message.setContent(
                "医生已为您开具“" + itemName + "”检查，检查申请编号为 "
                        + examinationId
                        + "。请线下完成检查，取得结果后在诊疗记录中提交给医生确认。"
        );
        message.setRelatedType(RELATED_EXAMINATION);
        message.setRelatedId(examinationId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 医生退回患者提交的检查结果后通知患者补充。 */
    public void createExaminationNeedsRevision(
            Long recipientUserId,
            Long examinationId,
            String reason
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_REMINDER);
        message.setTitle("检查结果需要补充");
        message.setContent(
                "检查申请 " + examinationId + " 已由医生退回，请补充后重新提交。原因："
                        + reason + "。"
        );
        message.setRelatedType(RELATED_EXAMINATION);
        message.setRelatedId(examinationId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }

    /** 检查报告生成后通知患者查看结果。 */
    public void createExaminationCompleted(
            Long recipientUserId,
            Long examinationId,
            Long reportId
    ) {
        Message message = new Message();
        message.setRecipientUserId(recipientUserId);
        message.setMessageType(TYPE_REMINDER);
        message.setTitle("检查报告已完成");
        message.setContent(
                "检查申请 " + examinationId + " 已生成报告，报告编号为 "
                        + reportId + "，可在诊疗记录中查看。"
        );
        message.setRelatedType(RELATED_EXAMINATION);
        message.setRelatedId(examinationId);
        message.setIsRead(false);
        messageMapper.insert(message);
    }
}
