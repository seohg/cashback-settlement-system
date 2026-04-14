package com.larva.cashback.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 카드
    LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "LIMIT_EXCEEDED", "카드 한도를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
