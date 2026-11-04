package com.yilian.madp.admin.queue.service;

import com.yilian.madp.admin.queue.dto.AdminQueueListQuery;
import com.yilian.madp.admin.queue.dto.AdjustQueueRequest;
import com.yilian.madp.admin.queue.mapper.AdminQueueMapper;
import com.yilian.madp.admin.queue.vo.AdminQueueListItemResponse;
import com.yilian.madp.admin.queue.vo.QueueAdjustLockResponse;
import com.yilian.madp.admin.queue.vo.QueueAdjustResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/** 管理员队列查询业务层。 */
@Service
@RequiredArgsConstructor
public class AdminQueueService {

    private static final Map<String, String> STATUS_MAPPING = Map.of(
            "WAITING", "等待接诊",
            "SIGNED", "等待接诊",
            "PROCESSING", "接诊中",
            "DONE", "完成",
            "COMPLETED", "完成"
    );

    private final AdminQueueMapper adminQueueMapper;

    public PageResult<AdminQueueListItemResponse> getQueues(
            AdminQueueListQuery query
    ) {
        query.setDoctorName(trimToNull(query.getDoctorName()));
        // 排队属于当天现场业务；未指定日期时默认只查询当天。
        if (query.getDate() == null) {
            query.setDate(LocalDate.now());
        }
        String status = trimToNull(query.getStatus());
        // 兼容前端英文枚举，同时允许直接传数据库中的中文状态。
        query.setStatus(status == null
                ? null : STATUS_MAPPING.getOrDefault(status, status));

        int total = adminQueueMapper.countQueues(query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminQueueListItemResponse> records =
                adminQueueMapper.selectQueues(
                        query, offset, query.getSize()
                );
        return new PageResult<>(records, total);
    }

    /**
     * 异常调序只改变 sort_no，不篡改患者的真实签到时间。
     * 整组等待队列会先加锁，再统一编号，避免重复序号和并发覆盖。
     */
    @Transactional
    public QueueAdjustResponse adjustQueue(
            Long adminId,
            Long queueId,
            AdjustQueueRequest request
    ) {
        QueueAdjustLockResponse target =
                adminQueueMapper.selectQueueForUpdate(queueId);
        if (target == null) {
            throw new BusinessException(ErrorCode.QUEUE_NOT_FOUND);
        }
        if (!"等待接诊".equals(target.status())) {
            throw new BusinessException(ErrorCode.QUEUE_STATUS_ERROR);
        }
        if (!LocalDate.now().equals(target.visitDate())) {
            throw new BusinessException(ErrorCode.QUEUE_DATE_ERROR);
        }

        List<Long> queueIds = adminQueueMapper.selectWaitingQueueIdsForUpdate(
                target.doctorId(), target.visitDate(), target.timePeriod()
        );
        int oldIndex = queueIds.indexOf(queueId);
        if (oldIndex < 0) {
            throw new BusinessException(ErrorCode.QUEUE_STATUS_ERROR);
        }
        int newPosition = request.position();
        if (newPosition > queueIds.size()) {
            throw new BusinessException(ErrorCode.QUEUE_POSITION_ERROR);
        }
        int oldPosition = oldIndex + 1;
        if (oldPosition == newPosition) {
            throw new BusinessException(ErrorCode.QUEUE_POSITION_UNCHANGED);
        }

        // 删除目标后插入新位置，区间内其他患者会自动前移或后移。
        queueIds.remove(oldIndex);
        queueIds.add(newPosition - 1, queueId);
        for (int index = 0; index < queueIds.size(); index++) {
            if (adminQueueMapper.updateSortNo(queueIds.get(index), index + 1) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT);
            }
        }

        String reason = request.reason().trim();
        adminQueueMapper.insertAdjustLog(
                adminId, queueId, oldPosition, newPosition, reason
        );
        return new QueueAdjustResponse(queueId, oldPosition, newPosition);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
