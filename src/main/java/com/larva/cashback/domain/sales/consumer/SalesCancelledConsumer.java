package com.larva.cashback.domain.sales.consumer;

import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesRepository;
import com.larva.cashback.domain.sales.event.SalesEvent;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesCancelledConsumer {

    private final SalesRepository salesRepository;
    private final ServiceApplicationRepository serviceApplicationRepository;

    /**
     * 취소건 처리
     *
     * 1. salesId로 원매출 조회
     * 2. 없으면 → 예외 던져서 Retry
     * 3. 있으면 → 연관 ServiceApplication 조회
     *    - 있으면 → isApplied=false 처리
     *    - 없으면 → 서비스 미적용 건 → 정상 종료
     */
    @KafkaListener(topics = "sales.cancelled", groupId = "cashback-group")
    @Transactional
    public void consume(SalesEvent event) {
        log.info("취소건 수신: salesId={}, cardId={}", event.getSalesId(), event.getCardId());

        // 1. 원매출 조회
        Optional<Sales> originalSales = salesRepository.findById(event.getSalesId());

        if (originalSales.isEmpty()) {
            // 순서 역전: 정상건이 아직 DB에 없음 → Retry
            log.warn("원매출 미존재 — 순서 역전 판단, Retry: salesId={}", event.getSalesId());
            throw new IllegalStateException("Original sales not found for salesId: " + event.getSalesId());
        }

        // 2. 연관 ServiceApplication 조회
        List<ServiceApplication> applications = serviceApplicationRepository.findBySalesId(event.getSalesId());

        if (applications.isEmpty()) {
            log.info("서비스 미적용 건, 취소 처리 불필요: salesId={}", event.getSalesId());
            return;
        }

        // 3. ServiceApplication 취소 처리
        applications.forEach(ServiceApplication::cancel);
        log.info("서비스 취소 완료: salesId={}, 취소건수={}", event.getSalesId(), applications.size());
    }
}