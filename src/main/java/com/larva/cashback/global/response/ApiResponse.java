package com.larva.cashback.global.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorResponse errorResponse;

    private ApiResponse(boolean success, T data, ErrorResponse errorResponse) {
        this.success = success;
        this.data = data;
        this.errorResponse = errorResponse;
    }
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    };

    public static ApiResponse<?> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }

}
