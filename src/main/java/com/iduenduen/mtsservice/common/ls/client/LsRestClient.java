package com.iduenduen.mtsservice.common.ls.client;

import com.iduenduen.mtsservice.common.ls.auth.LsTokenManager;
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
        return post(path, trCd, requestBody, responseType, "N", "");
    }

    public <T> T post(String path, String trCd, Object requestBody, Class<T> responseType, String trCont, String trContKey) {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + path)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + lsTokenManager.getToken())
                .header("tr_cd", trCd)
                .header("tr_cont", trCont)
                .header("tr_cont_key", trContKey == null ? "" : trContKey)
                .header("mac_address", lsProperties.getMacAddress() == null ? "" : lsProperties.getMacAddress())
                .body(requestBody)
                .retrieve()
                .body(responseType);
    }
}
