package com.larva.cashback.domain.serviceapplication;

import com.larva.cashback.domain.servicepolicy.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceApplicationRepository extends JpaRepository<ServiceApplication, Long> {

    List<ServiceApplication> findByServiceTypeAndIsAppliedTrue(ServiceType serviceType);

    List<ServiceApplication> findBySalesId(Long salesId);

    /**
     * 배치 조회: CASHBACK + isApplied=true + PENDING
     */
    @Query("SELECT sa FROM ServiceApplication sa " +
            "JOIN FETCH sa.sales s " +
            "JOIN FETCH s.card " +
            "WHERE sa.serviceType = :serviceType " +
            "AND sa.isApplied = true " +
            "AND sa.paymentStatus = :status")
    Page<ServiceApplication> findByServiceTypeAndIsAppliedTrueAndPaymentStatus(
            ServiceType serviceType, PaymentStatus status, Pageable pageable);
}