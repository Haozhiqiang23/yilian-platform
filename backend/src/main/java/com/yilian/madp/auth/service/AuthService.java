package com.yilian.madp.auth.service;

import com.yilian.madp.auth.dto.LoginRequest;
import com.yilian.madp.auth.dto.RegisterRequest;
import com.yilian.madp.auth.entity.SysUser;
import com.yilian.madp.auth.mapper.AuthMapper;
import com.yilian.madp.auth.security.JwtService;
import com.yilian.madp.auth.vo.LoginResponse;
import com.yilian.madp.auth.vo.RegisterResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import com.yilian.madp.patient.entity.Patient;
import com.yilian.madp.patient.mapper.PatientMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final PatientMapper patientMapper;

    private final VerifyCodeService verifyCodeService;

    public LoginResponse login(LoginRequest request) {

        SysUser user = authMapper.selectByUsername(
                request.getPhone()
        );

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        if (!"正常".equals(user.getStatus())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN
            );
        }

        String token = jwtService.createToken(
                user.getId(),
                user.getRole()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getRole(),
                user.getUsername()
        );
    }

    /**
     * 注册患者账号。
     *
     * 该方法同时写入 sys_user 和 patient 两张表，事务可以保证：
     * 任意一步失败时，两张表的新增操作会一起回滚。
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String phone = request.getPhone().trim();
        String idCard = request.getIdCard().trim().toUpperCase();
        String name = request.getName().trim();
        // 先检查业务唯一性，返回比数据库异常更容易理解的提示。
        if (authMapper.selectByUsername(phone) != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        if (patientMapper.countByIdCard(idCard) > 0) {
            throw new BusinessException(ErrorCode.ID_CARD_ALREADY_REGISTERED);
        }

        // 唯一性校验通过后再消费验证码，避免重复手机号消耗有效验证码。
        verifyCodeService.verifyAndConsume(
                request.getCodeId(),
                phone,
                request.getVerifyCode()
        );

        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("patient");
        user.setStatus("正常");
        authMapper.insert(user);

        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setName(name);
        patient.setIdCard(idCard);
        patient.setPhone(phone);
        patientMapper.insert(patient);

        return new RegisterResponse(user.getId(), phone, name);


    }
}
