package com.larva.cashback.domain.serviceapplication;

import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.servicepolicy.ServiceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id")
    private Sales sales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Column(nullable = false)
    private int benefitAmount;

    @Column(nullable = false)
    private boolean isApplied; // Y: 정상, N : 취소

    @Builder
    public ServiceApplication(Sales sales, ServiceType serviceType, int benefitAmount) {
        this.sales = sales;
        this.serviceType = serviceType;
        this.benefitAmount = benefitAmount;
        this.isApplied = true;
    }

    public void cancel() {
        this.isApplied = false;
    }

}
