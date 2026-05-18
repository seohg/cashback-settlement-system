package com.larva.cashback.domain.sales;

import com.larva.cashback.domain.sales.event.SalesCancelledEvent;
import com.larva.cashback.domain.sales.event.SalesCreatedEvent;
import com.larva.cashback.domain.sales.event.SalesEvent;
import com.larva.cashback.domain.sales.event.SalesEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SalesEventProducerTest {

    @InjectMocks
    private SalesEventProducer salesEventProducer;

    @Mock
    private KafkaTemplate<String, SalesEvent> kafkaTemplate;

    @Test
    @DisplayName("결제 이벤트 → sales.created 토픽 발행")
    void handleSalesCreated() {
        // given
        SalesEvent salesEvent = SalesEvent.builder()
                .salesId(1L)
                .cardId(100L)
                .cardProductCode("A00001")
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .build();

        SalesCreatedEvent event = new SalesCreatedEvent(salesEvent);

        // when
        salesEventProducer.handleSalesCreated(event);

        // then
        verify(kafkaTemplate).send("sales.created", "100", salesEvent);
    }

    @Test
    @DisplayName("취소 이벤트 → sales.cancelled 토픽 발행")
    void handleSalesCancelled() {
        // given
        SalesEvent salesEvent = SalesEvent.builder()
                .salesId(2L)
                .cardId(100L)
                .cardProductCode("A00001")
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50_000)
                .build();

        SalesCancelledEvent event = new SalesCancelledEvent(salesEvent);

        // when
        salesEventProducer.handleSalesCancelled(event);

        // then
        verify(kafkaTemplate).send("sales.cancelled", "100", salesEvent);
    }
}