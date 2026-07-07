// src/test/java/com/larva/cashback/domain/servicepolicy/ServicePolicyCacheServiceTest.java
package com.larva.cashback.domain.servicepolicy;

import com.larva.cashback.global.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class ServicePolicyCacheServiceTest {

    @Autowired
    private ServicePolicyCacheService servicePolicyCacheService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ServicePolicyRepository servicePolicyRepository;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("policy").clear();
    }

    @Test
    @DisplayName("DB 정상 조회 테스트")
    void firstCall_dbQueried() {
        // given
        given(servicePolicyRepository.findByCardProductCodeAndIsActiveTrue("A00001")).willReturn(List.of());

        // when
        servicePolicyCacheService.getPolicies("A00001");

        // then — DB 1번 조회
        verify(servicePolicyRepository, times(1)).findByCardProductCodeAndIsActiveTrue("A00001");
    }

    @Test
    @DisplayName("캐시 hit 시 DB 1회 조회 확인")
    void secondCall_cacheHit() {
        // given
        given(servicePolicyRepository.findByCardProductCodeAndIsActiveTrue("A00001"))
                .willReturn(List.of());

        // when
        servicePolicyCacheService.getPolicies("A00001");
        servicePolicyCacheService.getPolicies("A00001");

        // then — DB는 1번만 조회됨 (두 번째는 캐시에서)
        verify(servicePolicyRepository, times(1)).findByCardProductCodeAndIsActiveTrue("A00001");
    }

    @Test
    @DisplayName("evict 후 재조회")
    void afterEvict_dbQueriedAgain() {
        // given
        given(servicePolicyRepository.findByCardProductCodeAndIsActiveTrue("A00001")).willReturn(List.of());

        // when
        servicePolicyCacheService.getPolicies("A00001");
        servicePolicyCacheService.evict("A00001");
        servicePolicyCacheService.getPolicies("A00001");

        // then — DB 2번 조회
        verify(servicePolicyRepository, times(2)).findByCardProductCodeAndIsActiveTrue("A00001");
    }

    @Test
    @DisplayName("다른 cardProductCode ")
    void differentKeys_cachedIndependently() {
        // given
        given(servicePolicyRepository.findByCardProductCodeAndIsActiveTrue("A00001")).willReturn(List.of());
        given(servicePolicyRepository.findByCardProductCodeAndIsActiveTrue("B00001")).willReturn(List.of());

        // when
        servicePolicyCacheService.getPolicies("A00001");
        servicePolicyCacheService.getPolicies("A00001");
        servicePolicyCacheService.getPolicies("B00001");

        // then
        verify(servicePolicyRepository, times(1)).findByCardProductCodeAndIsActiveTrue("A00001");
        verify(servicePolicyRepository, times(1)).findByCardProductCodeAndIsActiveTrue("B00001");
    }
}