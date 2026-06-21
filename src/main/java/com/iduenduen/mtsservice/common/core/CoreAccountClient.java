package com.iduenduen.mtsservice.common.core;

import com.iduenduen.mtsservice.common.core.dto.CoreAccountHoldingsResponse;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
public class CoreAccountClient {

    private final RestTemplate restTemplate;

    @Value("${core.api.base-url}")
    private String coreBaseUrl;

    public AccountBalanceResponse getBalance(Long accountId) {
        String url = UriComponentsBuilder
                .fromUriString(coreBaseUrl + "/account/balance")
                .queryParam("accountId", accountId)
                .toUriString();

        return restTemplate.getForObject(url, AccountBalanceResponse.class);
    }

    public CoreAccountHoldingsResponse getHoldings(Long accountId) {
        String url = UriComponentsBuilder
                .fromUriString(coreBaseUrl + "/account/holdings")
                .queryParam("accountId", accountId)
                .toUriString();

        return restTemplate.getForObject(url, CoreAccountHoldingsResponse.class);
    }


}