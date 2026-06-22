package com.iduenduen.mtsservice.common.core;

import com.iduenduen.mtsservice.common.core.dto.CoreAccountHoldingsResponse;
import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class CoreAccountClient {

    private final RestTemplate restTemplate;

    @Value("${core.api.base-url}")
    private String coreBaseUrl;

    public AccountBalanceResponse getBalance(Long accountId, String authHeader) {
        String url = UriComponentsBuilder
                .fromUriString(coreBaseUrl + "/account/balance")
                .queryParam("accountId", accountId)
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(authHeader));
        ResponseEntity<ApiResponse<AccountBalanceResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<ApiResponse<AccountBalanceResponse>>() {});
        return response.getBody().getData();
    }

    public CoreAccountHoldingsResponse getHoldings(Long accountId, String authHeader) {
        String url = UriComponentsBuilder
                .fromUriString(coreBaseUrl + "/account/holdings")
                .queryParam("accountId", accountId)
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(authHeader));
        ResponseEntity<ApiResponse<CoreAccountHoldingsResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<ApiResponse<CoreAccountHoldingsResponse>>() {});
        return response.getBody().getData();
    }

    private HttpHeaders authHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        return headers;
    }
}