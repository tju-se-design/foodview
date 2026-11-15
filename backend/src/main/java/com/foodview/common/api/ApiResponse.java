package com.foodview.common.api;

/**
 * 所有业务接口的统一响应体。code 为 0 表示成功，非 0 取 {@link ErrorCode} 的值。
 */
public record ApiResponse<T>(int code, String message, T data) {

    private static final int SUCCESS = 0;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS, "ok", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(SUCCESS, "ok", null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null);
    }
}
