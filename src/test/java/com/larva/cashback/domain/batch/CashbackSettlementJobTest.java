package com.larva.cashback.domain.batch;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.card.CardRepository;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.member.MemberRepository;
import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesRepository;
import com.larva.cashback.domain.serviceapplication.PaymentStatus;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import com.larva.cashback.domain.servicepolicy.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CashbackSettlementJobTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job cashbackJob;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private ServiceApplicationRepository serviceApplicationRepository;

    private Card cardWithAddress;
    private Card cardWithoutAddress;

    @BeforeEach
    void setUp() {
        serviceApplicationRepository.deleteAll();
        salesRepository.deleteAll();
        cardRepository.deleteAll();
        memberRepository.deleteAll();

        // 매번 다른 이메일 사용
        String suffix = String.valueOf(System.currentTimeMillis());

        Member memberWithAddress = memberRepository.save(
                Member.builder()
                        .email("with-" + suffix + "@test.com")
                        .name("주소있음")
                        .address("서울시 강남구")
                        .build()
        );

        Member memberWithoutAddress = memberRepository.save(
                Member.builder()
                        .email("without-" + suffix + "@test.com")
                        .name("주소없음")
                        .build()
        );

        cardWithAddress = cardRepository.save(
                Card.builder()
                        .member(memberWithAddress)
                        .cardProductCode("A00001")
                        .cardNumber("1111-1111-1111-1111")
                        .cardLimit(10000000)
                        .build()
        );

        cardWithoutAddress = cardRepository.save(
                Card.builder()
                        .member(memberWithoutAddress)
                        .cardProductCode("A00001")
                        .cardNumber("2222-2222-2222-2222")
                        .cardLimit(10000000)
                        .build()
        );
    }

    @Test
    @DisplayName("정상건 → PENDING에서 PAID로 변경")
    void normalCase_pendingToPaid() throws Exception {
        // given
        Sales sales = salesRepository.save(Sales.builder()
                .card(cardWithAddress)
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .build());

        ServiceApplication application = serviceApplicationRepository.save(
                ServiceApplication.builder()
                        .sales(sales)
                        .serviceType(ServiceType.CASHBACK)
                        .benefitAmount(2500)
                        .build()
        );

        // when
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(cashbackJob, params);

        // then
        ServiceApplication result = serviceApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }


    @Test
    @DisplayName("999만원 초과 → 분할 처리")
    void splitCase_benefitAmountOver999() throws Exception {
        // given
        Sales sales = salesRepository.save(Sales.builder()
                .card(cardWithAddress)
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .build());

        ServiceApplication application = serviceApplicationRepository.save(
                ServiceApplication.builder()
                        .sales(sales)
                        .serviceType(ServiceType.CASHBACK)
                        .benefitAmount(15000000)  // 1500만원 → 999만 + 501만
                        .build()
        );

        // when
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(cashbackJob, params);

        // then — 원본은 SKIPPED, 분할된 2건이 PAID로 새로 생성
        ServiceApplication original = serviceApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(original.getPaymentStatus()).isEqualTo(PaymentStatus.SKIPPED);

        List<ServiceApplication> allApplications = serviceApplicationRepository.findAll();
        List<ServiceApplication> paidOnes = allApplications.stream()
                .filter(a -> a.getPaymentStatus() == PaymentStatus.PAID)
                .toList();

        assertThat(paidOnes).hasSize(2);
        assertThat(paidOnes.get(0).getBenefitAmount()).isEqualTo(9999999);
        assertThat(paidOnes.get(1).getBenefitAmount()).isEqualTo(5000001);
    }

    @Test
    @DisplayName("주소 미등록 → 해당 건만 skip, 배치 전체는 정상 완료")
    void addressNotFound_skipAndContinue() throws Exception {
        // given — 정상건 + 주소없는 건
        Sales normalSales = salesRepository.save(Sales.builder()
                .card(cardWithAddress)
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .build());

        Sales noAddressSales = salesRepository.save(Sales.builder()
                .card(cardWithoutAddress)
                .merchantCode("M001")
                .merchantCategory("FOOD")
                .amount(50000)
                .installmentMonth(0)
                .build());

        ServiceApplication normalApp = serviceApplicationRepository.save(
                ServiceApplication.builder()
                        .sales(normalSales)
                        .serviceType(ServiceType.CASHBACK)
                        .benefitAmount(2500)
                        .build()
        );

        ServiceApplication noAddressApp = serviceApplicationRepository.save(
                ServiceApplication.builder()
                        .sales(noAddressSales)
                        .serviceType(ServiceType.CASHBACK)
                        .benefitAmount(2500)
                        .build()
        );

        // when
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(cashbackJob, params);

        // then — 정상건은 PAID, 주소없는 건은 PENDING 유지 (skip됨)
        ServiceApplication normalResult = serviceApplicationRepository
                .findById(normalApp.getId()).orElseThrow();
        ServiceApplication noAddressResult = serviceApplicationRepository
                .findById(noAddressApp.getId()).orElseThrow();

        assertThat(normalResult.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(noAddressResult.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

        // 배치 자체는 COMPLETED
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    }
}