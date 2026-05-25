package com.larva.cashback.domain.sales.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DltConsumer {

    @KafkaListener(topics = "sales.created.DLT", groupId = "cashback-dlt-group")
    public void handleCreatedDlt(String message) {
        log.error("[DLT] 심사 처리 실패 — sales.created.DLT: {}", message);
    }

    @KafkaListener(topics = "sales.cancelled.DLT", groupId = "cashback-dlt-group")
    public void handleCancelledDlt(String message) {
        log.error("[DLT] 취소 처리 실패 — sales.cancelled.DLT: {}", message);
    }
}