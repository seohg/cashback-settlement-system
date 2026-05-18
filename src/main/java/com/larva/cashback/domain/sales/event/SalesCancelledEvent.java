package com.larva.cashback.domain.sales.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SalesCancelledEvent {
    private final SalesEvent salesEvent;
}
