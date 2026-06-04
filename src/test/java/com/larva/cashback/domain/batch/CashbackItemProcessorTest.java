package com.larva.cashback.domain.batch;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.serviceapplication.PaymentStatus;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.servicepolicy.ServiceType;
import com.larva.cashback.global.exception.AddressNotFoundException;
import com.larva.cashback.domain.batch.CashbackItemProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashbackItemProcessorTest {

    private final CashbackItemProcessor processor = new CashbackItemProcessor();


    @Test
    @DisplayName("취소건인데 isApplied=true → SKIPPED")
    void cancelledSales_skip() throws Exception {
        ServiceApplication item = createApplication(createCancelledSales(), 2_500);

        // when
        List<ServiceApplication> result = processor.process(item);

        // then
        assertThat(result).isNull();
        assertThat(item.getPaymentStatus()).isEqualTo(PaymentStatus.SKIPPED);
    }

    @Test
    @DisplayName("주소 미등록 → AddressNotFoundException")
    void noAddress_throwsException() {
        // given — address가 null인 Member
        ServiceApplication item = createApplication(
                createSalesWithMember(null), 2500);

        // when & then
        assertThatThrownBy(() -> processor.process(item))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    @DisplayName("999만원 이하 → 정상 처리")
    void normalAmount_paid() throws Exception {
        // given
        ServiceApplication item = createApplication(createNormalSales(), 5000000);

        // when
        List<ServiceApplication> result = processor.process(item);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBenefitAmount()).isEqualTo(5000000);
        assertThat(result.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("1500만원 → 999만원 + 501만원+ 원본 SKIPPED")
    void splitIntoTwo() throws Exception {
        // given
        ServiceApplication item = createApplication(createNormalSales(), 15000000);

        // when
        List<ServiceApplication> result = processor.process(item);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBenefitAmount()).isEqualTo(9999999);
        assertThat(result.get(1).getBenefitAmount()).isEqualTo(5000001);
        assertThat(result.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.get(1).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(item.getPaymentStatus()).isEqualTo(PaymentStatus.SKIPPED);
    }

    @Test
    @DisplayName("2500만원 → 999만원 + 999만원 + 502만원 (3건 분할)")
    void splitIntoThree() throws Exception {
        // given
        ServiceApplication item = createApplication(createNormalSales(), 25000000);

        // when
        List<ServiceApplication> result = processor.process(item);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getBenefitAmount()).isEqualTo(9999999);
        assertThat(result.get(1).getBenefitAmount()).isEqualTo(9999999);
        assertThat(result.get(2).getBenefitAmount()).isEqualTo(5000002);
    }

    private ServiceApplication createApplication(Sales sales, int benefitAmount) {
        return ServiceApplication.builder()
                .sales(sales)
                .serviceType(ServiceType.CASHBACK)
                .benefitAmount(benefitAmount)
                .build();
    }

    private Sales createNormalSales() {
        return createSalesWithMember("서울시 강남구");
    }

    private Sales createSalesWithMember(String address) {
        Member member = Member.builder()
                .email("larva@larva.com")
                .name("김라바")
                .address(address)
                .build();

        Card card = Card.builder()
                .member(member)
                .cardProductCode("A00001")
                .cardNumber("1234-5678-9101-1121")
                .cardLimit(1000000)
                .build();

        return Sales.builder()
                .card(card)
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .build();
    }

    private Sales createCancelledSales() {
        Sales original = createNormalSales();
        return Sales.builder()
                .card(original.getCard())
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .originalSales(original)
                .build();
    }
}