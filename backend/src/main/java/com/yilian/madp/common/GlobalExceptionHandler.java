package com.yilian.madp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
//这个类可以统一处理所有 Controller 抛出的异常。
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 【并发排查：服务端诊断】保留异常调用栈供定位500，不向前端返回内部错误详情。
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理我们主动抛出的业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception){
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getCode())
                .body(Result.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 处理请求参数校验异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception){
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message;

        if (fieldError == null) {
            message = ErrorCode.BAD_REQUEST.getMessage();
        } else if (fieldError.isBindingFailure()) {
            // 日期、数字等参数无法转换成目标 Java 类型。
            message = "请求参数格式不正确";
        } else {
            // @NotBlank、@Pattern、@Size 等注解提供的业务提示。
            message = fieldError.getDefaultMessage();
        }
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    /**
     * 处理 URL 查询参数类型错误，例如把 date 传成非日期字符串。
     * 不把 Spring 内部类型转换信息直接暴露给前端。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(Result.error(
                        ErrorCode.BAD_REQUEST.getCode(),
                        "请求参数格式不正确"
                ));
    }

    /**
     * 处理没有明确处理的其他异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception){
        log.error("未处理的业务请求异常", exception);
        return ResponseEntity
                .internalServerError()
                .body(Result.error(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getMessage()
                ));
    }


}
