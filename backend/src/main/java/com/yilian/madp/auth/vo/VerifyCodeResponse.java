package com.yilian.madp.auth.vo;

/**
 * 获取验证码后的返回数据。
 */
public class VerifyCodeResponse {

    private final String codeId;
    private final long ttlSeconds;

    public VerifyCodeResponse(String codeId, long ttlSeconds) {
        this.codeId = codeId;
        this.ttlSeconds = ttlSeconds;
    }

    public String getCodeId() {
        return codeId;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
