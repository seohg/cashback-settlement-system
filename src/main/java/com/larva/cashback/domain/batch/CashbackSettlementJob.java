package com.larva.cashback.domain.batch;

import com.larva.cashback.domain.serviceapplication.PaymentStatus;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import com.larva.cashback.domain.servicepolicy.ServiceType;
import com.larva.cashback.global.exception.AddressNotFoundException;
import com.larva.cashback.global.exception.CancelledSalesException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CashbackSettlementJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ServiceApplicationRepository serviceApplicationRepository;
    private final CashbackItemProcessor cashbackItemProcessor;
    private final CashbackItemWriter cashbackItemWriter;

    @Bean
    public Job cashbackJob() {
        return new JobBuilder("cashbackSettlementJob", jobRepository)
                .start(cashbackStep())
                .build();
    }

    @Bean
    public Step cashbackStep() {
        return new StepBuilder("cashbackSettlementStep", jobRepository)
                .<ServiceApplication, List<ServiceApplication>>chunk(1000, transactionManager)
                .reader(cashbackItemReader())
                .processor(cashbackItemProcessor)
                .writer(cashbackItemWriter)
                .faultTolerant()
                .skip(AddressNotFoundException.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public RepositoryItemReader<ServiceApplication> cashbackItemReader() {
        return new RepositoryItemReaderBuilder<ServiceApplication>()
                .name("cashbackItemReader")
                .repository(serviceApplicationRepository)
                .methodName("findByServiceTypeAndIsAppliedTrueAndPaymentStatus")
                .arguments(List.of(ServiceType.CASHBACK, PaymentStatus.PENDING))
                .pageSize(1000)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }
}