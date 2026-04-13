package com.larva.cashback.global.exception;

public class AddressNotFoundException extends BusinessException {
    public AddressNotFoundException() {
        super("ADDRESS_NOT_FOUND", "주소 미등록 회원입니다.");
    }
}
