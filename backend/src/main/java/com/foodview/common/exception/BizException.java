package com.foodview.common.exception;

import com.foodview.common.api.ErrorCode;

/**
 * 预期内的业务失败，由 {@link GlobalExceptionHandler} 转成统一响应体，不打印堆栈。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
