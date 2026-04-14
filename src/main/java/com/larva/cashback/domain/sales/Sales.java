package com.larva.cashback.domain.sales;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sales extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private String merchantCategory;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int installmentMonth;

    @Column(nullable = false)
    private boolean isCancelled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_sales_id")
    private Sales originalSales;

    @Builder
    public Sales(Card card, String merchantCategory, int amount, int installmentMonth, Sales originalSales) {
        this.card = card;
        this.merchantCategory = merchantCategory;
        this.amount = amount;
        this.installmentMonth = installmentMonth;
        this.isCancelled = originalSales != null;
        this.originalSales = originalSales;
    }
}

