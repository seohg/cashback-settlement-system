package com.larva.cashback.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 카드
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "카드를 찾을 수 없습니다."),
    CARD_BLOCKED(HttpStatus.BAD_REQUEST, "CARD_BLOCKED", "차단된 카드입니다."),
    LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "LIMIT_EXCEEDED", "카드 한도를 초과했습니다."),

    // 매출
    SALES_NOT_FOUND(HttpStatus.NOT_FOUND, "SALES_NOT_FOUND", "매출내역을 찾을 수 없습니다."),
    SALES_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "SALES_ALREADY_CANCELLED", "이미 취소된 매출내역입니다."),

    // 배치
    ADDRESS_NOT_FOUND(HttpStatus.BAD_REQUEST, "ADDRESS_NOT_FOUND", "주소 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}