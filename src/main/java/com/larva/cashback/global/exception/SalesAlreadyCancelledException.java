package com.larva.cashback.global.exception;

public class SalesAlreadyCancelledException extends BusinessException {
    public SalesAlreadyCancelledException() {
        super(ErrorCode.SALES_ALREADY_CANCELLED);
    }
}