package com.yilian.madp.admin.log.service;

import com.yilian.madp.admin.log.dto.AdminLogListQuery;
import com.yilian.madp.admin.log.mapper.AdminLogMapper;
import com.yilian.madp.admin.log.vo.AdminLogListItemResponse;
import com.yilian.madp.admin.log.vo.AdminLogDetailResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 管理员只读操作日志查询业务层。 */
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private static final Map<String, String> OPERATION_MAPPING = Map.of(
            "审核医生", "DOCTOR_REVIEW",
            "处理退款", "REFUND_PROCESS",
            "调整队列", "QUEUE_ADJUST",
            "状态修改", "STATUS"
    );

    private final AdminLogMapper adminLogMapper;

    public PageResult<AdminLogListItemResponse> getLogs(AdminLogListQuery query) {
        query.setKeyword(trimToNull(query.getKeyword()));
        String operation = trimToNull(query.getOperation());
        query.setOperation(operation == null
                ? null : OPERATION_MAPPING.getOrDefault(operation, operation));

        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        int total = adminLogMapper.countLogs(query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<AdminLogListItemResponse> records = adminLogMapper.selectLogs(
                query, offset, query.getSize()
        );
        return new PageResult<>(records, total);
    }

    /** 按主键读取完整日志；审计日志只能查询，不能修改或删除。 */
    public AdminLogDetailResponse getLogDetail(Long id) {
        AdminLogDetailResponse detail = adminLogMapper.selectLogById(id);
        if (detail == null) {
            throw new BusinessException(ErrorCode.OPERATION_LOG_NOT_FOUND);
        }
        return detail;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
