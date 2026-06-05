package com.larva.cashback.domain.servicepolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicePolicyCacheService {

    private final ServicePolicyRepository servicePolicyRepository;

    @Cacheable(value = "policy", key = "#cardProductCode")
    public List<ServicePolicy> getPolicies(String cardProductCode) {
        log.info("Redis 캐시 미스 — DB 조회: cardProductCode={}", cardProductCode);
        return servicePolicyRepository.findByCardProductCodeAndIsActiveTrue(cardProductCode);
    }

    @CacheEvict(value = "policy", key = "#cardProductCode")
    public void evict(String cardProductCode) {
        log.info("Redis 캐시 무효화: cardProductCode={}", cardProductCode);
    }
}