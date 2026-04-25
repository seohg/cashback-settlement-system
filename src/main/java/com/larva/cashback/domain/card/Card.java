package com.larva.cashback.domain.card;

import com.larva.cashback.domain.member.Member;
import com.larva.cashback.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Table(name = "card")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String cardProductCode;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private int cardLimit;

    @Column(nullable = false)
    private int usedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Builder
    public Card(Member member, String cardProductCode, String cardNumber, int cardLimit) {
        this.member = member;
        this.cardProductCode = cardProductCode;
        this.cardNumber = cardNumber;
        this.cardLimit = cardLimit;
        this.usedAmount = 0;
        this.status = CardStatus.ACTIVE;
    }
    public boolean isBlocked() {
        return this.status == CardStatus.BLOCKED;
    }

    public boolean isPayable(int amount) {
        return (this.usedAmount + amount) <= this.cardLimit;
    }
    public void use(int amount) {
        this.usedAmount += amount;
    }

    public void restore(int amount) {
        this.usedAmount -= amount;
    }
}