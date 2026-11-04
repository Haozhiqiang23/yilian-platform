package com.yilian.madp.patient.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yilian.madp.auth.entity.SysUser;
import com.yilian.madp.auth.mapper.AuthMapper;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.patient.dto.UpdatePatientProfileRequest;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import com.yilian.madp.patient.vo.PatientProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 患者个人信息业务层，负责查询校验和 Entity 到 VO 的转换。
 */
@Service
@RequiredArgsConstructor
public class PatientProfileService {

    private final PatientMapper patientMapper;

    private final AuthMapper authMapper;

    /**
     * 根据当前登录用户 ID 查询其患者资料。
     */
    public PatientProfileResponse getProfile(Long userId) {
        Patient patient = patientMapper.selectByUserId(userId);

        // 登录账号存在，但没有关联患者资料时，返回明确的业务错误。
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_PROFILE_NOT_FOUND);
        }

        // Entity 服务于数据库，VO 服务于前端，两者分开可避免暴露内部字段。
        return toProfileResponse(patient);
    }

    /**
     * 修改当前患者的手机号和联系地址。
     *
     * patient.phone 保存患者联系方式，sys_user.username 是登录账号；
     * 手机号变更时必须同步两张表，否则页面显示的新手机号不能用于登录。
     */
    @Transactional
    public PatientProfileResponse updateProfile(
            Long userId,
            UpdatePatientProfileRequest request
    ) {
        Patient patient = patientMapper.selectByUserId(userId);
        if (patient == null) {
            throw new BusinessException(ErrorCode.PATIENT_PROFILE_NOT_FOUND);
        }

        String phone = request.getPhone().trim();
        String address = request.getAddress() == null
                ? null
                : request.getAddress().trim();

        // 简单单表条件查询使用 MyBatis-Plus Wrappers，无需手写 SQL。
        SysUser phoneOwner = authMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, phone)
        );

        // 查到其他账号使用该手机号时，禁止更新。
        if (phoneOwner != null && !phoneOwner.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        // 兼容历史数据：patient.phone 也必须保证没有被其他患者占用。
        Long samePhonePatientCount = patientMapper.selectCount(
                Wrappers.<Patient>lambdaQuery()
                        .eq(Patient::getPhone, phone)
                        .ne(Patient::getId, patient.getId())
        );

        if (samePhonePatientCount > 0) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        patient.setPhone(phone);
        patient.setAddress(address);
        patientMapper.updateById(patient);

        // 登录账号与患者联系电话保持一致。
        SysUser user = authMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        user.setUsername(phone);
        authMapper.updateById(user);

        return toProfileResponse(patient);
    }

    /** Entity 转 VO，避免 Controller 直接返回数据库实体。 */
    private PatientProfileResponse toProfileResponse(Patient patient) {
        return new PatientProfileResponse(
                patient.getId(),
                patient.getName(),
                patient.getPhone(),
                patient.getIdCard(),
                patient.getAddress()
        );
    }
}
