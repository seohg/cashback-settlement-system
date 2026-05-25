package com.larva.cashback.domain.servicepolicy;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesRepository;
import com.larva.cashback.domain.sales.event.SalesEvent;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceReviewServiceTest {

    @InjectMocks
    private ServiceReviewService serviceReviewService;

    @Mock
    private ServicePolicyRepository servicePolicyRepository;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private ServiceApplicationRepository serviceApplicationRepository;

    private Sales sales;
    private SalesEvent event;

    @BeforeEach
    void setUp() {
        Member member = Member.builder().email("larva@larva.com").name("김라바").build();
        Card card = Card.builder()
                .member(member)
                .cardProductCode("A00001")
                .cardNumber("1234-5678-9101-1121")
                .cardLimit(1000000)
                .build();

        sales = Sales.builder()
                .card(card)
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .build();

        event = SalesEvent.builder()
                .salesId(1L)
                .cardId(1L)
                .cardProductCode("A00001")
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .build();
    }


    @Nested
    @DisplayName("조건 매칭")
    class ConditionMatching {

        @Test
        @DisplayName("merchantCode + merchantCategory 둘 다 일치 → 적용")
        void exactMatch() {
            // given
            ServicePolicy policy = createPolicy("M001", "FOOD", ServiceType.CASHBACK, 0.05, 1);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository).save(any(ServiceApplication.class));
        }

        @Test
        @DisplayName("merchantCode=null → 적용")
        void nullMerchantCode() {
            // given
            ServicePolicy policy = createPolicy(null, "FOOD", ServiceType.CASHBACK, 0.05, 1);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository).save(any(ServiceApplication.class));
        }

        @Test
        @DisplayName("merchantCategory=null → 적용")
        void nullMerchantCategory() {
            // given
            ServicePolicy policy = createPolicy("M001", null, ServiceType.CASHBACK, 0.05, 1);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository).save(any(ServiceApplication.class));
        }

        @Test
        @DisplayName("둘 다 null → 적용")
        void bothNull() {
            // given
            ServicePolicy policy = createPolicy(null, null, ServiceType.POINT, 0.01, 1);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository).save(any(ServiceApplication.class));
        }

        @Test
        @DisplayName("merchantCode 불일치 → 미적용")
        void merchantCodeMismatch() {
            // given — 정책은 M002 가맹점, 매출은 M001
            ServicePolicy policy = createPolicy("M002", "FOOD", ServiceType.CASHBACK, 0.05, 1);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("merchantCategory 불일치 → 미적용")
        void merchantCategoryMismatch() {
            // given — 정책은 GAS 업종, 매출은 FOOD
            ServicePolicy policy = createPolicy("M001", "GAS", ServiceType.CASHBACK, 0.05, 1);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("conditionAmount 미달 → 미적용")
        void amountBelowCondition() {
            // given — 조건금액 100,000원, 매출금액 50,000원
            ServicePolicy policy = createPolicy("M001", "FOOD", ServiceType.CASHBACK, 0.05, 1, 100_000);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("아무 정책도 매칭 안 됨 → ServiceApplication 미생성")
        void noPolicyMatched() {
            // given
            stubPolicies(List.of());

            // when
            serviceReviewService.review(event);

            // then
            verify(serviceApplicationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("우선순위 + serviceType별 선택")
    class PrioritySelection {

        @Test
        @DisplayName("같은 serviceType → priority 낮은 것 1개만 선택")
        void selectHighestPriority() {
            // given — 같은 CASHBACK인데 priority 1 vs 2
            ServicePolicy policy1 = createPolicy(null, null, ServiceType.CASHBACK, 0.02, 2); // 낮은 우선순위
            ServicePolicy policy2 = createPolicy("M001", "FOOD", ServiceType.CASHBACK, 0.05, 1); // 높은 우선순위
            stubPolicies(List.of(policy1, policy2));

            // when
            serviceReviewService.review(event);

            // then — 1개만 생성
            ArgumentCaptor<ServiceApplication> captor = ArgumentCaptor.forClass(ServiceApplication.class);
            verify(serviceApplicationRepository, times(1)).save(captor.capture());

            // priority 1인 정책의 benefitRate(0.05) 적용 확인
            assertThat(captor.getValue().getBenefitAmount()).isEqualTo(2_500); // 50000 * 0.05
        }

        @Test
        @DisplayName("다른 serviceType → 각각 1개씩 생성")
        void differentServiceTypes() {
            // given — CASHBACK + POINT
            ServicePolicy cashback = createPolicy("M001", "FOOD", ServiceType.CASHBACK, 0.05, 1);
            ServicePolicy point = createPolicy(null, null, ServiceType.POINT, 0.01, 1);
            stubPolicies(List.of(cashback, point));

            // when
            serviceReviewService.review(event);

            // then — 2개 생성
            verify(serviceApplicationRepository, times(2)).save(any(ServiceApplication.class));
        }
    }


    @Nested
    @DisplayName("금액 계산")
    class BenefitCalculation {

        @Test
        @DisplayName("benefitAmount가 maxBenefitAmount 초과 시 max로 제한")
        void maxBenefitCap() {
            // given — rate 50%, max 10,000원, 매출 50,000원 → 계산 25,000원 → max 10,000원
            ServicePolicy policy = createPolicyWithMax("M001", "FOOD", ServiceType.CASHBACK, 0.50, 1, 0, 10_000);
            stubPolicies(List.of(policy));

            // when
            serviceReviewService.review(event);

            // then
            ArgumentCaptor<ServiceApplication> captor = ArgumentCaptor.forClass(ServiceApplication.class);
            verify(serviceApplicationRepository).save(captor.capture());
            assertThat(captor.getValue().getBenefitAmount()).isEqualTo(10_000);
        }
    }

    private void stubPolicies(List<ServicePolicy> policies) {
        given(salesRepository.findById(any())).willReturn(Optional.of(sales));
        given(serviceReviewService.getCachedPolicies("A00001")).willReturn(policies);
    }

    private ServicePolicy createPolicy(String merchantCode, String merchantCategory,
                                       ServiceType serviceType, double benefitRate, int priority) {
        return createPolicy(merchantCode, merchantCategory, serviceType, benefitRate, priority, 0);
    }

    private ServicePolicy createPolicy(String merchantCode, String merchantCategory,
                                       ServiceType serviceType, double benefitRate,
                                       int priority, int conditionAmount) {
        return createPolicyWithMax(merchantCode, merchantCategory, serviceType,
                benefitRate, priority, conditionAmount, 9_999_999);
    }

    private ServicePolicy createPolicyWithMax(String merchantCode, String merchantCategory,
                                              ServiceType serviceType, double benefitRate,
                                              int priority, int conditionAmount, int maxBenefitAmount) {
        return ServicePolicy.builder()
                .cardProductCode("A00001")
                .serviceType(serviceType)
                .merchantCode(merchantCode)
                .merchantCategory(merchantCategory)
                .conditionAmount(conditionAmount)
                .benefitRate(benefitRate)
                .maxBenefitAmount(maxBenefitAmount)
                .priority(priority)
                .validFrom(LocalDateTime.of(2024, 1, 1, 0, 0))
                .validTo(LocalDateTime.of(2099, 12, 31, 23, 59))
                .build();
    }
}