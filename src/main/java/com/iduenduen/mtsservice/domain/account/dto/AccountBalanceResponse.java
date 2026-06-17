package com.iduenduen.mtsservice.domain.account.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountBalanceResponse {
    private Long availableCash;
    private Long d0Balance;
    private Long d1Balance;
    private Long d2Balance;
}
