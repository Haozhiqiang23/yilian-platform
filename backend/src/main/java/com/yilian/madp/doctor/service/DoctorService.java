package com.yilian.madp.doctor.service;

import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.doctor.dto.DoctorListQuery;
import com.yilian.madp.doctor.mapper.DoctorMapper;
import com.yilian.madp.doctor.vo.DoctorListItemResponse;
import com.yilian.madp.doctor.vo.DoctorDetailResponse;
import com.yilian.madp.doctor.vo.DoctorProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医生查询业务层，负责整理查询条件和计算分页偏移量。
 */
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorMapper doctorMapper;
    private final DoctorDetailCache doctorDetailCache;

    public PageResult<DoctorListItemResponse> getDoctorPage(
            DoctorListQuery query
    ) {
        // 去掉姓名关键字两端空格，避免空格影响模糊查询结果。
        if (query.getName() != null) {
            query.setName(query.getName().trim());
        }

        int total = doctorMapper.countEnabled(query);
        int offset = (query.getPage() - 1) * query.getSize();

        List<DoctorListItemResponse> records =
                doctorMapper.selectEnabledPage(
                        query,
                        offset,
                        query.getSize()
                );

        return new PageResult<>(records, total);
    }

    /**
     * 查询启用医生详情，隐藏停用医生和不存在的医生。
     */
    public DoctorDetailResponse getDoctorDetail(Long doctorId) {
        DoctorDetailResponse doctor =
                doctorDetailCache.get(doctorId, () -> doctorMapper.selectEnabledDetail(doctorId));

        if (doctor == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }

        return doctor;
    }

    /** 查询当前登录账号关联的医生资料。 */
    public DoctorProfileResponse getProfile(Long userId) {
        DoctorProfileResponse profile =
                doctorMapper.selectProfileByUserId(userId);
        if (profile == null) {
            throw new BusinessException(ErrorCode.DOCTOR_NOT_FOUND);
        }
        return profile;
    }
}
