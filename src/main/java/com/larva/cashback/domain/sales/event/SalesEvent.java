package com.larva.cashback.domain.sales.event;

import com.larva.cashback.domain.sales.Sales;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SalesEvent {
    private Long salesId;
    private Long cardId;
    private String cardProductCode;
    private String merchantCode;
    private String merchantCategory;
    private int amount;

    @Builder
    public SalesEvent(Long salesId, Long cardId,  String cardProductCode,
                      String merchantCode, String merchantCategory, int amount) {
        this.salesId = salesId;
        this.cardId = cardId;
        this.cardProductCode = cardProductCode;
        this.merchantCode = merchantCode;
        this.merchantCategory = merchantCategory;
        this.amount = amount;
    }
    public static SalesEvent from(Sales sales) {
        return SalesEvent.builder()
                .salesId(sales.getId())
                .cardId(sales.getCard().getId())
                .cardProductCode(sales.getCard().getCardProductCode())
                .merchantCode(sales.getMerchantCode())
                .merchantCategory(sales.getMerchantCategory())
                .amount(sales.getAmount())
                .build();
    }

}
