package com.iduenduen.mtsservice.domain.account.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountBalanceResponse {
    private Long availableAmt;
}