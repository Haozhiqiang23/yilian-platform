package com.yilian.madp.auth.service;

import com.yilian.madp.auth.vo.VerifyCodeResponse;
import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
//没看
/**
 * 开发阶段的内存验证码服务。
 *
 * 当前固定验证码为 123456，有效期 5 分钟。
 * 后续接入 Redis 和短信平台时，只需要替换本服务的存取实现。
 */
@Service
public class VerifyCodeService {

    private static final String DEVELOPMENT_CODE = "123456";
    private static final long TTL_SECONDS = 300;

    private final ConcurrentHashMap<String, VerifyCodeRecord> records =
            new ConcurrentHashMap<>();

    /**
     * 为手机号生成唯一 codeId，防止不同手机号之间错用验证码。
     */
    public VerifyCodeResponse create(String phone) {
        String codeId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(TTL_SECONDS);

        records.put(
                codeId,
                new VerifyCodeRecord(phone, DEVELOPMENT_CODE, expiresAt)
        );

        return new VerifyCodeResponse(codeId, TTL_SECONDS);
    }

    /**
     * 同时校验 codeId、手机号、验证码和有效期，校验成功后立即删除。
     */
    public void verifyAndConsume(String codeId, String phone, String verifyCode) {
        VerifyCodeRecord record = records.get(codeId);

        boolean invalid = record == null
                || Instant.now().isAfter(record.expiresAt())
                || !record.phone().equals(phone)
                || !record.code().equals(verifyCode);

        if (invalid) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_INVALID);
        }

        // 验证码只能使用一次，防止重复注册请求反复消费。
        records.remove(codeId);
    }

    private record VerifyCodeRecord(
            String phone,
            String code,
            Instant expiresAt
    ) {
    }
}
