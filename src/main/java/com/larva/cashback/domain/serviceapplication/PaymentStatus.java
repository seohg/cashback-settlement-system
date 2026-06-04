package com.larva.cashback.domain.serviceapplication;

public enum PaymentStatus {
    PENDING,    // 지급 대기 (심사 완료, 배치 미처리)
    PAID,       // 지급 완료
    SKIPPED
}