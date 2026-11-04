package com.yilian.madp.source.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.doctor.entity.Doctor;
import com.yilian.madp.doctor.mapper.DoctorMapper;
import com.yilian.madp.source.dto.SourceListQuery;
import com.yilian.madp.source.mapper.SourceMapper;
import com.yilian.madp.source.vo.SourceResponse;
import com.yilian.madp.payment.service.PaymentExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 号源查询业务层。
 */
@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceMapper sourceMapper;
    private final DoctorMapper doctorMapper;
    private final PaymentExpirationService paymentExpirationService;

    public List<SourceResponse> getSources(SourceListQuery query) {
        paymentExpirationService.expirePendingAppointments();

        /*
         * 先判断医生是否有效：
         * 医生无效返回 404；医生有效但指定日期无号源则正常返回空数组。
         */
        /** 判断医生是否存在且处于启用状态。 */
        Long count = doctorMapper.selectCount(
                Wrappers.<Doctor>lambdaQuery()
                        .eq(Doctor::getId,query.getDoctorId())
                        .eq(Doctor::getStatus,"启用")
        );
        if (count == 0) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }

        return sourceMapper.selectSources(query);
    }
}
