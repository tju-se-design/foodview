package com.foodview.common.api;

/**
 * 业务错误码。目前码值与 HTTP 状态码同值，出现与 HTTP 语义不同的业务错误时再另起号段。
 */
public enum ErrorCode {

    INVALID_PARAM(400, "参数不合法"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
