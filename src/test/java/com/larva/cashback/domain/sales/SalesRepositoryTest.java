package com.larva.cashback.domain.sales;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.card.CardRepository;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class SalesRepositoryTest {

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Card card;

    @BeforeEach
        // 각 테스트 실행 전에 공통 데이터 세팅
    void setUp() {
        Member member = Member.builder()
                .email("larvq@larva.com")
                .name("김라바")
                .build();
        memberRepository.save(member);

        Card card = Card.builder()
                .member(member)
                .cardProductCode("A300012")
                .cardNumber("1234-5678-9101-1121")
                .cardLimit(100000000)
                .build();
        cardRepository.save(card);
    }

    @Test
    @DisplayName("일반 결제건 저장")
    void saveNormalSales() {
        // given
        Sales sales = Sales.builder()
                .card(card)
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .originalSales(null)
                .build();

        // when
        Sales saved = salesRepository.save(sales);
        Sales found = salesRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getMerchantCategory()).isEqualTo("FOOD");
        assertThat(found.isCancelled()).isFalse();
        assertThat(found.getOriginalSales()).isNull();
    }

    @Test
    @DisplayName("취소건 저장")
    void saveCancelSales() {
        // given
        Sales originalSales = Sales.builder()
                .card(card)
                .merchantCategory("FOOD")
                .amount(50_000)
                .installmentMonth(0)
                .originalSales(null)
                .build();
        salesRepository.save(originalSales);

        Sales cancelSales = Sales.builder()
                .card(card)
                .merchantCategory("FOOD")
                .amount(50_000)
                .installmentMonth(0)
                .originalSales(originalSales)
                .build();

        // when
        Sales saved = salesRepository.save(cancelSales);
        Sales found = salesRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.isCancelled()).isTrue();
        assertThat(found.getOriginalSales().getId()).isEqualTo(originalSales.getId());
    }
}
