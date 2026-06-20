package com.iduenduen.mtsservice.common.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class CoreAccountHoldingsResponse {
    private Long availableAmt;
    private List<HoldingDto> holdings;

    @Getter
    @NoArgsConstructor
    public static class HoldingDto {
        private Long etfId;
        private int qty;
        private Long avgBuyPrice;
    }
}