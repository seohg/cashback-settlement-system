package com.larva.cashback.domain.sales.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesEventProducer {
    private static final String TOPIC_SALES_CREATED = "sales.created";
    private static final String TOPIC_SALES_CANCELLED = "sales.cancelled";

    private final KafkaTemplate<String, SalesEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSalesCreated(SalesCreatedEvent event) {
        SalesEvent salesEvent = event.getSalesEvent();
        String key = String.valueOf(salesEvent.getCardId());

        kafkaTemplate.send(TOPIC_SALES_CREATED, key, salesEvent);
        log.info("Kafka 발행 [sales.created] salesId={}, cardId={}", salesEvent.getSalesId(), key);

    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSalesCancelled(SalesCancelledEvent event) {
        SalesEvent salesEvent = event.getSalesEvent();
        String key = String.valueOf(salesEvent.getCardId());

        kafkaTemplate.send(TOPIC_SALES_CANCELLED, key, salesEvent);
        log.info("Kafka 발행 [sales.cancelled] salesId={}, cardId={}", salesEvent.getSalesId(), key);
    }

}
