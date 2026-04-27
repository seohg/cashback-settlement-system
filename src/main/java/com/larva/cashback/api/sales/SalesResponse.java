package com.larva.cashback.api.sales;

import com.larva.cashback.domain.sales.Sales;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SalesResponse {

    private Long salesId;
    private Long cardId;
    private String merchantCode;
    private String merchantCategory;
    private int amount;
    private int installmentMonth;
    private boolean isCancelled;
    private Long originalSalesId;
    private LocalDateTime createdAt;

    public static SalesResponse from(Sales sales) {
        return SalesResponse.builder()
                .salesId(sales.getId())
                .cardId(sales.getCard().getId())
                .merchantCode(sales.getMerchantCode())
                .merchantCategory(sales.getMerchantCategory())
                .amount(sales.getAmount())
                .installmentMonth(sales.getInstallmentMonth())
                .isCancelled(sales.isCancelled())
                .originalSalesId(sales.getOriginalSales() != null
                        ? sales.getOriginalSales().getId() : null)
                .createdAt(sales.getCreatedAt())
                .build();
    }
}