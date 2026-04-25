package com.larva.cashback.global.exception;

public class SalesNotFoundException extends BusinessException {
    public SalesNotFoundException() {
        super(ErrorCode.SALES_NOT_FOUND);
    }
}