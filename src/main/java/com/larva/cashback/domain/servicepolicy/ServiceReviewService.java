package com.larva.cashback.domain.servicepolicy;


import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesRepository;
import com.larva.cashback.domain.sales.event.SalesEvent;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceReviewService {

    private final ServicePolicyRepository servicePolicyRepository;
    private final SalesRepository salesRepository;
    private final ServiceApplicationRepository serviceApplicationRepository;

    /**
     * 서비스 심사
     *
     * 1. Sales 조회
     * 2. 정책 조회
     * 3. 조건 필터링
     * 4. serviceType별 최우선 정책 1개씩 선택
     * 5. ServiceApplication 생성
     */
    @Transactional
    public void review(SalesEvent event) {
        // 1. Sales 조회
        Sales sales = salesRepository.findById(event.getSalesId())
                .orElseThrow(() -> new IllegalArgumentException("Sales not found: " + event.getSalesId()));

        // 2. 정책 조회 (Redis 캐시)
        List<ServicePolicy> policies = getCachedPolicies(event.getCardProductCode());

        // 3. 조건 필터링
        LocalDateTime now = LocalDateTime.now();
        List<ServicePolicy> matchedPolicies = policies.stream()
                .filter(p -> matchesMerchantCode(p, event.getMerchantCode()))
                .filter(p -> matchesMerchantCategory(p, event.getMerchantCategory()))
                .filter(p -> event.getAmount() >= p.getConditionAmount())
                .filter(p -> !now.isBefore(p.getValidFrom()) && !now.isAfter(p.getValidTo()))
                .toList();

        // 4. serviceType별 최우선 정책 1개씩 선택
        Map<ServiceType, ServicePolicy> bestPolicies = matchedPolicies.stream()
                .collect(Collectors.toMap(
                        ServicePolicy::getServiceType,
                        p -> p,
                        (p1, p2) -> p1.getPriority() <= p2.getPriority() ? p1 : p2
                ));

        // 5. ServiceApplication 생성
        bestPolicies.forEach((serviceType, policy) -> {
            int benefitAmount = calculateBenefit(event.getAmount(), policy);

            ServiceApplication application = ServiceApplication.builder()
                    .sales(sales)
                    .serviceType(serviceType)
                    .benefitAmount(benefitAmount)
                    .build();

            serviceApplicationRepository.save(application);
            log.info("서비스 적용: salesId={}, type={}, amount={}",
                    event.getSalesId(), serviceType, benefitAmount);
        });

        if (bestPolicies.isEmpty()) {
            log.info("적용 가능한 정책 없음: salesId={}", event.getSalesId());
        }
    }

    /**
     * Redis 캐시 조회 — cardProductCode 기준
     */
    @Cacheable(value = "policy", key = "#cardProductCode")
    public List<ServicePolicy> getCachedPolicies(String cardProductCode) {
        log.info("Redis 캐시 미스 — DB 조회: cardProductCode={}", cardProductCode);
        return servicePolicyRepository.findByCardProductCodeAndIsActiveTrue(cardProductCode);
    }
    /**
     * merchantCode 조건 매칭
     */
    private boolean matchesMerchantCode(ServicePolicy policy, String merchantCode) {
        return policy.getMerchantCode() == null || policy.getMerchantCode().equals(merchantCode);
    }

    /**
     * merchantCategory 조건 매칭
     */
    private boolean matchesMerchantCategory(ServicePolicy policy, String merchantCategory) {
        return policy.getMerchantCategory() == null || policy.getMerchantCategory().equals(merchantCategory);
    }

    /**
     * 혜택 금액 계산
     */
    private int calculateBenefit(int amount, ServicePolicy policy) {
        int calculated = (int) (amount * policy.getBenefitRate());
        return Math.min(calculated, policy.getMaxBenefitAmount());
    }
}
