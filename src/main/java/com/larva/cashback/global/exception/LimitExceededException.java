package com.larva.cashback.global.exception;

public class LimitExceededException extends BusinessException {
    public LimitExceededException() {
        super(ErrorCode.LIMIT_EXCEEDED);
    }
}
