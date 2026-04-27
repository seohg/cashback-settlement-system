package com.larva.cashback.api.sales;

import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesService;
import com.larva.cashback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    /**
     * 결제 생성 API
     * POST /api/sales
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SalesResponse>> createSales(
            @Valid @RequestBody SalesCreateRequest request) {

        Sales sales = salesService.createSales(
                request.getCardId(),
                request.getMerchantCode(),
                request.getMerchantCategory(),
                request.getAmount(),
                request.getInstallmentMonth()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(SalesResponse.from(sales)));
    }

    /**
     * 결제 취소 API
     * POST /api/sales/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SalesResponse>> cancelSales(@PathVariable Long id) {

        Sales cancelledSales = salesService.cancelSales(id);

        return ResponseEntity.ok(ApiResponse.ok(SalesResponse.from(cancelledSales)));
    }
}