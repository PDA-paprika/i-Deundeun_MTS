package com.iduenduen.mtsservice.common.core;

import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class CoreAccountClient {

    private final RestTemplate restTemplate;

    private static final String CORE_BASE_URL = "http://localhost:8081";

    public AccountBalanceResponse getBalance(Long accountId) {
        String url = UriComponentsBuilder
                .fromUriString(CORE_BASE_URL + "/account/balance")
                .queryParam("accountId", accountId)
                .toUriString();

        return restTemplate.getForObject(url, AccountBalanceResponse.class);
    }
}