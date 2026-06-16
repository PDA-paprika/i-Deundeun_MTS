package com.iduenduen.mtsservice.domain.ls.client;

import com.iduenduen.mtsservice.domain.ls.auth.LsTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class LsRestClient {

    private final RestClient restClient;
    private final LsProperties lsProperties;
    private final LsTokenManager lsTokenManager;

    public <T> T post(String path, String trCd, Object requestBody, Class<T> responseType) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + path)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + lsTokenManager.getToken())
                .header("tr_cd", trCd)
                .header("tr_cont", "N")
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }
}
