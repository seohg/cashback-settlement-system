package com.larva.cashback.domain.servicepolicy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicePolicyRepository extends JpaRepository<ServicePolicy, Long> {
    List<ServicePolicy> findByCardProductCodeAndIsActiveTrue(String cardProductCode);
}
