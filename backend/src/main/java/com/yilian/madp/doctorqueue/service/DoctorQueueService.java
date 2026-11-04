package com.yilian.madp.doctorqueue.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.doctor.entity.Doctor;
import com.yilian.madp.doctor.mapper.DoctorMapper;
import com.yilian.madp.doctorqueue.dto.DoctorQueueQuery;
import com.yilian.madp.doctorqueue.mapper.DoctorQueueMapper;
import com.yilian.madp.doctorqueue.vo.DoctorQueueItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** 医生查看本人患者队列业务层。 */
@Service
@RequiredArgsConstructor
public class DoctorQueueService {

    private static final String DOCTOR_ENABLED = "启用";

    private final DoctorMapper doctorMapper;
    private final DoctorQueueMapper doctorQueueMapper;

    public PageResult<DoctorQueueItemResponse> getMyQueue(
            Long userId,
            DoctorQueueQuery query
    ) {
        // doctorId 始终由登录用户反查，绝不接收前端传入的医生 ID。
        Doctor doctor = doctorMapper.selectOne(
                Wrappers.<Doctor>lambdaQuery()
                        .eq(Doctor::getUserId, userId)
                        .eq(Doctor::getStatus, DOCTOR_ENABLED)
        );
        if (doctor == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }

        if (query.getStatus() != null) {
            String status = query.getStatus().trim();
            query.setStatus(status.isEmpty() ? null : status);
        }

        LocalDate today = LocalDate.now();
        int total = doctorQueueMapper.countMine(doctor.getId(), today, query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<DoctorQueueItemResponse> records = doctorQueueMapper.selectMine(
                doctor.getId(),
                today,
                query,
                offset,
                query.getSize()
        );

        return new PageResult<>(records, total);
    }
}
