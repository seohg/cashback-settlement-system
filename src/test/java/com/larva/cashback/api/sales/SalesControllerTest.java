package com.larva.cashback.api.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larva.cashback.domain.card.Card;
import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.sales.Sales;
import com.larva.cashback.domain.sales.SalesService;
import com.larva.cashback.global.exception.CardBlockedException;
import com.larva.cashback.global.exception.CardNotFoundException;
import com.larva.cashback.global.exception.LimitExceededException;
import com.larva.cashback.global.exception.SalesAlreadyCancelledException;
import com.larva.cashback.global.exception.SalesNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesController.class)
class SalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SalesService salesService;

    @Nested
    @DisplayName("POST /api/sales")
    class CreateSalesApi {

        @Test
        @DisplayName("정상 결제 → 201 + ApiResponse 포맷")
        void success() throws Exception {
            // given
            Sales sales = createSales();
            given(salesService.createSales(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(sales);

            SalesCreateRequest request = SalesCreateRequest.builder()
                    .cardId(1L)
                    .merchantCode("M001")
                    .merchantCategory("FOOD")
                    .amount(50000)
                    .installmentMonth(0)
                    .build();

            // when & then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.merchantCode").value("M001"))
                    .andExpect(jsonPath("$.data.merchantCategory").value("FOOD"))
                    .andExpect(jsonPath("$.data.amount").value(50000));
        }

        @Test
        @DisplayName("없는 카드 → 404 + CARD_NOT_FOUND")
        void cardNotFound() throws Exception {
            // given
            given(salesService.createSales(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willThrow(new CardNotFoundException());

            SalesCreateRequest request = SalesCreateRequest.builder()
                    .cardId(999L)
                    .merchantCode("M001")
                    .merchantCategory("FOOD")
                    .amount(50_000)
                    .installmentMonth(0)
                    .build();

            // when & then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorResponse.code").value("CARD_NOT_FOUND"));
        }

        @Test
        @DisplayName("BLOCKED 카드 → 400 + CARD_BLOCKED")
        void cardBlocked() throws Exception {
            // given
            given(salesService.createSales(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willThrow(new CardBlockedException());

            SalesCreateRequest request = SalesCreateRequest.builder()
                    .cardId(1L)
                    .merchantCode("M001")
                    .merchantCategory("FOOD")
                    .amount(50000)
                    .installmentMonth(0)
                    .build();

            // when & then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorResponse.code").value("CARD_BLOCKED"));
        }

        @Test
        @DisplayName("한도 초과 → 400 + LIMIT_EXCEEDED")
        void limitExceeded() throws Exception {
            // given
            given(salesService.createSales(anyLong(), anyString(), anyString(), anyInt(), anyInt()))
                    .willThrow(new LimitExceededException());

            SalesCreateRequest request = SalesCreateRequest.builder()
                    .cardId(1L)
                    .merchantCode("M001")
                    .merchantCategory("FOOD")
                    .amount(5000000)
                    .installmentMonth(0)
                    .build();

            // when & then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorResponse.code").value("LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("@Valid 실패 — cardId null → 400")
        void validationFailCardId() throws Exception {
            // given — cardId 누락
            String body = """
                    {
                        "merchantCode": "M001",
                        "merchantCategory": "FOOD",
                        "amount": 50000,
                        "installmentMonth": 0
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorResponse.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("@Valid 실패 — merchantCode 빈값 → 400")
        void validationFailMerchantCode() throws Exception {
            // given
            SalesCreateRequest request = SalesCreateRequest.builder()
                    .cardId(1L)
                    .merchantCode("")
                    .merchantCategory("FOOD")
                    .amount(50000)
                    .installmentMonth(0)
                    .build();

            // when & then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorResponse.code").value("INVALID_INPUT"));
        }
    }


    @Nested
    @DisplayName("POST /api/sales/{id}/cancel")
    class CancelSalesApi {

        @Test
        @DisplayName("정상 취소 → 200 + isCancelled=true")
        void success() throws Exception {
            // given
            Sales cancelledSales = createCancelledSales();
            given(salesService.cancelSales(anyLong()))
                    .willReturn(cancelledSales);

            // when & then
            mockMvc.perform(post("/api/sales/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.cancelled").value(true));
        }

        @Test
        @DisplayName("없는 Sales 취소 → 404 + SALES_NOT_FOUND")
        void salesNotFound() throws Exception {
            // given
            given(salesService.cancelSales(anyLong()))
                    .willThrow(new SalesNotFoundException());

            // when & then
            mockMvc.perform(post("/api/sales/999/cancel"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorResponse.code").value("SALES_NOT_FOUND"));
        }

        @Test
        @DisplayName("이미 취소된 건 → 400 + SALES_ALREADY_CANCELLED")
        void alreadyCancelled() throws Exception {
            // given
            given(salesService.cancelSales(anyLong()))
                    .willThrow(new SalesAlreadyCancelledException());

            // when & then
            mockMvc.perform(post("/api/sales/1/cancel"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorResponse.code").value("SALES_ALREADY_CANCELLED"));
        }
    }


    private Sales createSales() {
        Member member = Member.builder().email("larvakim@larva.com").name("김라바").build();
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
        Sales original = createSales();
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