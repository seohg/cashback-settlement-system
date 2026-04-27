package com.larva.cashback.api.sales;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SalesCreateRequest {

    @NotNull(message = "카드 ID는 필수입니다.")
    private Long cardId;

    @NotBlank(message = "가맹점코드는 필수입니다.")
    private String merchantCode;

    @NotBlank(message = "업종코드는 필수입니다.")
    private String merchantCategory;

    @Min(value = 1, message = "결제금액은 1원 이상이어야 합니다.")
    private int amount;

    @Min(value = 0, message = "할부개월수는 0 이상이어야 합니다.")
    private int installmentMonth;

    @Builder
    public SalesCreateRequest(Long cardId, String merchantCode, String merchantCategory,
                              int amount, int installmentMonth) {
        this.cardId = cardId;
        this.merchantCode = merchantCode;
        this.merchantCategory = merchantCategory;
        this.amount = amount;
        this.installmentMonth = installmentMonth;
    }
}