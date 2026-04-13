package com.larva.cashback.global.response;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String message;
    private final String code;
    public ErrorResponse(String cpde, String message) {
        this.code = cpde;
        this.message = message;
    }
}
