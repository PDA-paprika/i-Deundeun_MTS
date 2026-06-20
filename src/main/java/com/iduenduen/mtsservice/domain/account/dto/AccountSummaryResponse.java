package com.iduenduen.mtsservice.domain.account.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class AccountSummaryResponse {
    private Long availableAmt;
    private Long totalPurchaseAmount;
    private List<HoldingDto> holdings;

    @Getter
    @Builder
    public static class HoldingDto {
        private Long etfId;
        private String etfCode;
        private String etfName;
        private String logoImg;
        private int qty;
        private Long avgBuyPrice;
    }
}