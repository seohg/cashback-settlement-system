package com.larva.cashback.domain.sales;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.card.CardRepository;
import com.larva.cashback.domain.card.CardStatus;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import com.larva.cashback.domain.servicepolicy.ServiceType;
import com.larva.cashback.global.exception.CardBlockedException;
import com.larva.cashback.global.exception.CardNotFoundException;
import com.larva.cashback.global.exception.LimitExceededException;
import com.larva.cashback.global.exception.SalesAlreadyCancelledException;
import com.larva.cashback.global.exception.SalesNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @InjectMocks
    private SalesService salesService;

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ServiceApplicationRepository serviceApplicationRepository;

    private Member member;
    private Card card;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("larvakim@larva.com")
                .name("김라바")
                .build();

        card = Card.builder()
                .member(member)
                .cardProductCode("A00001")
                .cardNumber("1234-5678-9101-1121")
                .cardLimit(1000000)
                .build();
    }

    @Nested
    @DisplayName("결제 생성")
    class CreateSales {

        @Test
        @DisplayName("정상 결제 시, Sales 생성 후 usedAmount 업데이트")
        void success() {
            // given
            given(cardRepository.findByIdWithLock(anyLong()))
                    .willReturn(Optional.of(card));
            given(salesRepository.save(any(Sales.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Sales result = salesService.createSales(1L, "M001", "FOOD", 50000, 0);

            // then
            assertThat(result.getMerchantCode()).isEqualTo("M001");
            assertThat(result.getMerchantCategory()).isEqualTo("FOOD");
            assertThat(result.getAmount()).isEqualTo(50000);
            assertThat(result.isCancelled()).isFalse();
            assertThat(card.getUsedAmount()).isEqualTo(50000);

            verify(salesRepository).save(any(Sales.class));
        }

        @Test
        @DisplayName("없는 카드 → CardNotFoundException")
        void cardNotFound() {
            // given
            given(cardRepository.findByIdWithLock(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    salesService.createSales(999L, "M001", "FOOD", 50000, 0))
                    .isInstanceOf(CardNotFoundException.class);

            verify(salesRepository, never()).save(any());
        }

        @Test
        @DisplayName("BLOCKED 카드 → CardBlockedException")
        void cardBlocked() {
            // given
            Card blockedCard = createBlockedCard();
            given(cardRepository.findByIdWithLock(anyLong()))
                    .willReturn(Optional.of(blockedCard));

            // when & then
            assertThatThrownBy(() ->
                    salesService.createSales(1L, "M001", "FOOD", 50000, 0))
                    .isInstanceOf(CardBlockedException.class);

            verify(salesRepository, never()).save(any());
        }

        @Test
        @DisplayName("한도 초과 → LimitExceededException")
        void limitExceeded() {
            // given — 한도 100만원, 이미 90만원 사용
            card.use(900_000);
            given(cardRepository.findByIdWithLock(anyLong()))
                    .willReturn(Optional.of(card));

            // when & then — 15만원 추가 결제 시도 (90 + 15 = 105 > 100)
            assertThatThrownBy(() ->
                    salesService.createSales(1L, "M001", "FOOD", 150000, 0))
                    .isInstanceOf(LimitExceededException.class);

            verify(salesRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("결제 취소")
    class CancelSales {

        // SalesServiceTest — CancelSales.success() 수정

        @Test
        @DisplayName("정상 취소 시 취소건 Sales 생성 + ServiceApplication 취소 + usedAmount 차감")
        void success() {
            // given
            Sales originalSales = Sales.builder()
                    .card(card)
                    .merchantCode("M001")
                    .merchantCategory("FOOD")
                    .amount(50_000)
                    .installmentMonth(0)
                    .build();

            ServiceApplication application = ServiceApplication.builder()
                    .sales(originalSales)
                    .serviceType(ServiceType.CASHBACK)
                    .benefitAmount(2_500)
                    .build();

            card.use(50_000);

            given(salesRepository.findById(any()))              // anyLong() → any()
                    .willReturn(Optional.of(originalSales));
            given(serviceApplicationRepository.findBySalesId(any()))  // anyLong() → any()
                    .willReturn(List.of(application));
            given(cardRepository.findByIdWithLock(any()))        // anyLong() → any()
                    .willReturn(Optional.of(card));
            given(salesRepository.save(any(Sales.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Sales result = salesService.cancelSales(1L);

            // then
            assertThat(result.isCancelled()).isTrue();
            assertThat(result.getOriginalSales()).isEqualTo(originalSales);
            assertThat(application.isApplied()).isFalse();
            assertThat(card.getUsedAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("없는 Sales 취소 → SalesNotFoundException")
        void salesNotFound() {
            // given
            given(salesRepository.findById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> salesService.cancelSales(999L))
                    .isInstanceOf(SalesNotFoundException.class);
        }

        @Test
        @DisplayName("이미 취소된 건 재취소 → SalesAlreadyCancelledException")
        void alreadyCancelled() {
            // given — 취소건(isCancelled=true)
            Sales cancelledSales = Sales.builder()
                    .card(card)
                    .merchantCode("M001")
                    .merchantCategory("FOOD")
                    .amount(50_000)
                    .installmentMonth(0)
                    .originalSales(Sales.builder()
                            .card(card)
                            .merchantCode("M001")
                            .merchantCategory("FOOD")
                            .amount(50_000)
                            .installmentMonth(0)
                            .build())
                    .build();

            given(salesRepository.findById(anyLong()))
                    .willReturn(Optional.of(cancelledSales));

            // when & then
            assertThatThrownBy(() -> salesService.cancelSales(1L))
                    .isInstanceOf(SalesAlreadyCancelledException.class);
        }
    }

    private Card createBlockedCard() {
        Card blockedCard = Card.builder()
                .member(member)
                .cardProductCode("A00001")
                .cardNumber("9999-9999-9999-9999")
                .cardLimit(1000000)
                .build();
        try {
            java.lang.reflect.Field statusField = Card.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(blockedCard, CardStatus.BLOCKED);
        } catch (Exception e) {
            throw new RuntimeException("테스트 Card 상태 변경 실패", e);
        }
        return blockedCard;
    }
}