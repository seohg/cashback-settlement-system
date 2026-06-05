package com.larva.cashback.global.exception;

public class CancelledSalesException extends RuntimeException {
    public CancelledSalesException(Long salesId) {
        super("취소건 이중검증 skip: salesId=" + salesId);
    }
}