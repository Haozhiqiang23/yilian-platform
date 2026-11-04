package com.yilian.madp.common;
//自定义异常类
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
//        表示异常内部保存具体的错误码。
        super(errorCode.getMessage());
//        把错误提示交给父类 RuntimeException 保存。
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}