package com.larva.cashback.batch;

import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.global.exception.AddressNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CashbackItemProcessor implements ItemProcessor<ServiceApplication, List<ServiceApplication>> {

    private static final int MAX_BENEFIT_AMOUNT = 9999999;  // 캐시백 최대값

    /**
     * 캐시백 정산 처리
     *
     * 1. 이중 검증 — Sales.isCancelled 재확인 (DLT 미처리건 최종 보완)
     * 2. 주소 미등록 체크 — AddressNotFoundException (skip 대상)
     * 3. 금액 분할 — 999만원 초과 시 자동 분할
     *
     */
    @Override
    public List<ServiceApplication> process(ServiceApplication item) throws Exception {

        // 1. 이중 검증 — 취소건인데 isApplied=true로 남아있는 경우 (DLT 누락건)
        if (item.getSales().isCancelled()) {
            log.warn("[이중검증] 취소건 skip: salesId={}, serviceApplicationId={}", item.getSales().getId(), item.getId());
            item.skip();
            return null;
        }

        // 2. 주소 미등록 체크 (실무에서는 회원 주소 조회)
        if (item.getSales().getCard().getMember() != null && item.getSales().getCard().getMember().getAddress() == null) {
            throw new AddressNotFoundException();
        }

        // 3. 금액 분할
        if (item.getBenefitAmount() > MAX_BENEFIT_AMOUNT) {
            return splitBenefit(item);
        }

        // 정상 처리
        item.markAsPaid();
        return List.of(item);
    }

    /**
     * 금액 분할 — 999만원 초과 시 7자리 단위로 분할
     */
    private List<ServiceApplication> splitBenefit(ServiceApplication original) {
        List<ServiceApplication> splitList = new ArrayList<>();
        int remaining = original.getBenefitAmount();

        log.info("[금액분할] salesId={}, 원래금액={}", original.getSales().getId(), remaining);

        while (remaining > MAX_BENEFIT_AMOUNT) {
            ServiceApplication split = ServiceApplication.builder()
                    .sales(original.getSales())
                    .serviceType(original.getServiceType())
                    .benefitAmount(MAX_BENEFIT_AMOUNT)
                    .build();
            split.markAsPaid();
            splitList.add(split);
            remaining -= MAX_BENEFIT_AMOUNT;
        }

        // 나머지 금액
        ServiceApplication last = ServiceApplication.builder()
                .sales(original.getSales())
                .serviceType(original.getServiceType())
                .benefitAmount(remaining)
                .build();
        last.markAsPaid();
        splitList.add(last);

        // 원본은 skip 처리 (분할건으로 대체)
        original.skip();

        log.info("[금액분할] 분할 완료: {}건으로 분할", splitList.size());
        return splitList;
    }
}