package com.larva.cashback.global.exception;

public class AddressNotFoundException extends BusinessException {
    public AddressNotFoundException() {
        super(ErrorCode.LIMIT_EXCEEDED);
    }
}
