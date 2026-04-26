package com.larva.cashback.domain.sales;

import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.card.CardRepository;
import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import com.larva.cashback.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesService {

    private final CardRepository cardRepository;
    private final SalesRepository salesRepository;
    private final ServiceApplicationRepository serviceApplicationRepository;


    /**
    * 매입
    *
    * 1. 카드 조회
    * 2. 카드 정상 여부 체크
    * 3. 카드 한도 체크
    * 4. 매입내역 생성
    * 5. 한도 업데이트
    */
    @Transactional
    public Sales createSales(Long cardId, String merchantCode, String merchantCategory, int amount, int installmentMonth) {

        // 1. 카드 조회
        Card card = cardRepository.findByIdWithLock(cardId).orElseThrow(CardNotFoundException::new);

        // 2. 카드 정상 여부 체크
        if(card.isBlocked()){
            throw new CardBlockedException();
        }

        // 3. 카드 한도 체크
        if (!card.isPayable(amount)) {
            throw new LimitExceededException();
        }

        // 4. 매입내역 생성
        Sales sales = Sales.builder()
                .card(card)
                .merchantCode(merchantCode)
                .merchantCategory(merchantCategory)
                .amount(amount)
                .installmentMonth(installmentMonth)
                .build();

        salesRepository.save(sales);

        // 5. 한도 업데이트
        card.use(amount);

        return sales;
    }
    /**
     * 매입 취소
     *
     * 1. 원매출 조회
     * 2. 기 취소건 체크
     * 3. 취소 내역 생성
     * 4. 서비스 적용 취소
     * 5. 카드 한도 복원
     */
    @Transactional
    public Sales cancelSales(Long salesId) {

        // 1. 원매출 조회
        Sales originalSales = salesRepository.findById(salesId).orElseThrow(SalesNotFoundException::new);

        // 2. 기 취소건 체크
        if (originalSales.isCancelled()) {
            throw new SalesAlreadyCancelledException();
        }

        // 3. 취소 내역 생성
        Sales cancelSales = Sales.builder()
                .card(originalSales.getCard())
                .merchantCode(originalSales.getMerchantCode())
                .merchantCategory(originalSales.getMerchantCategory())
                .amount(originalSales.getAmount())
                .installmentMonth(originalSales.getInstallmentMonth())
                .originalSales(originalSales)
                .build();

        salesRepository.save(cancelSales);

        // 4. 서비스 적용 취소
        List<ServiceApplication> applications = serviceApplicationRepository.findBySalesId(originalSales.getId());
        applications.forEach(ServiceApplication::cancel);

        // 5. 카드 한도 복원
        Card card = cardRepository.findByIdWithLock(originalSales.getCard().getId()).orElseThrow(CardNotFoundException::new);
        card.restore(originalSales.getAmount());

        return cancelSales;
    }

}
