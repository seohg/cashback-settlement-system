package com.larva.cashback.domain.servicepolicy;

import com.larva.cashback.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "service_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServicePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardProductCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Column
    private String merchantCategory;

    @Column(nullable = false)
    private int conditionAmount;

    @Column(nullable = false)
    private double benefitRate;

    @Column(nullable = false)
    private int maxBenefitAmount;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Builder
    public ServicePolicy(String cardProductCode, ServiceType serviceType,
                         String merchantCategory, int conditionAmount,
                         double benefitRate, int maxBenefitAmount,
                         int priority, LocalDateTime validFrom, LocalDateTime validTo) {
        this.cardProductCode = cardProductCode;
        this.serviceType = serviceType;
        this.merchantCategory = merchantCategory;
        this.conditionAmount = conditionAmount;
        this.benefitRate = benefitRate;
        this.maxBenefitAmount = maxBenefitAmount;
        this.priority = priority;
        this.isActive = true;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }


}

