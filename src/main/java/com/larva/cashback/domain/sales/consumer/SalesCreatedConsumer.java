package com.larva.cashback.domain.sales.consumer;

import com.larva.cashback.domain.sales.event.SalesEvent;
import com.larva.cashback.domain.servicepolicy.ServiceReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesCreatedConsumer {

    private final ServiceReviewService serviceReviewService;

    /**
     * 정상 결제건 서비스 심사
     */
    @KafkaListener(topics = "sales.created", groupId = "cashback-group")
    public void consume(SalesEvent event) {
        log.info("결제건 수신: salesId={}, cardId={}", event.getSalesId(), event.getCardId());
        serviceReviewService.review(event);
    }
}
