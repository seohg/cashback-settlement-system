package com.larva.cashback.domain.serviceapplication;

import com.larva.cashback.domain.servicepolicy.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceApplicationRepository extends JpaRepository<ServiceApplication, Long> {

    List<ServiceApplication> findByServiceTypeAndIsAppliedTrue(ServiceType serviceType);

    List<ServiceApplication> findBySalesId(Long salesId);
}