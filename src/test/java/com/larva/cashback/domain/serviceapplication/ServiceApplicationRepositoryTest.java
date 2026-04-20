package com.larva.cashback.domain.serviceapplication;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.card.CardRepository;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.member.MemberRepository;
import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesRepository;
import com.larva.cashback.domain.servicepolicy.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class ServiceApplicationRepositoryTest {

    @Autowired
    private ServiceApplicationRepository serviceApplicationRepository;

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Sales sales;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .email("larva.kim@larva.com")
                .name("김라바")
                .build();
        memberRepository.save(member);

        Card card = Card.builder()
                .member(member)
                .cardProductCode("A300012")
                .cardNumber("1234-5678-9101-1121")
                .cardLimit(1000000)
                .build();
        cardRepository.save(card);

        sales = Sales.builder()
                .card(card)
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .originalSales(null)
                .build();
        salesRepository.save(sales);
    }

    @Test
    @DisplayName("ServiceApplication 저장 후 조회")
    void saveServiceApplication() {
        // given
        ServiceApplication application = ServiceApplication.builder()
                .sales(sales)
                .serviceType(ServiceType.CASHBACK)
                .benefitAmount(2500)
                .build();

        // when
        ServiceApplication saved = serviceApplicationRepository.save(application);
        ServiceApplication found = serviceApplicationRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getServiceType()).isEqualTo(ServiceType.CASHBACK);
        assertThat(found.isApplied()).isTrue();
        assertThat(found.getBenefitAmount()).isEqualTo(2500);
    }

    @Test
    @DisplayName("정상 취소 Test")
    void cancelServiceApplication() {
        // given
        ServiceApplication application = ServiceApplication.builder()
                .sales(sales)
                .serviceType(ServiceType.CASHBACK)
                .benefitAmount(2500)
                .build();
        serviceApplicationRepository.save(application);

        // when
        application.cancel();
        serviceApplicationRepository.save(application);
        ServiceApplication found = serviceApplicationRepository.findById(application.getId()).orElseThrow();

        // then
        assertThat(found.isApplied()).isFalse();
    }

    @Test
    @DisplayName("캐시백 배치 대상건 정상 조회 확인")
    void findCashbackApplied() {
        // given
        serviceApplicationRepository.save(ServiceApplication.builder()
                .sales(sales).serviceType(ServiceType.CASHBACK).benefitAmount(2500).build());
        serviceApplicationRepository.save(ServiceApplication.builder()
                .sales(sales).serviceType(ServiceType.POINT).benefitAmount(500).build());

        // when
        List<ServiceApplication> result = serviceApplicationRepository
                .findByServiceTypeAndIsAppliedTrue(ServiceType.CASHBACK);

        // then
        assertThat(result.get(0).getServiceType()).isEqualTo(ServiceType.CASHBACK);
    }
}
