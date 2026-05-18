package com.larva.cashback.domain.sales.event;

import com.larva.cashback.domain.sales.Sales;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SalesCreatedEvent {
    private final SalesEvent salesEvent;
}
