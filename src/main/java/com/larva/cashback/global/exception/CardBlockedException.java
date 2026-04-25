package com.larva.cashback.global.exception;

public class CardBlockedException extends BusinessException {
    public CardBlockedException() {
        super(ErrorCode.CARD_BLOCKED);
    }
}
