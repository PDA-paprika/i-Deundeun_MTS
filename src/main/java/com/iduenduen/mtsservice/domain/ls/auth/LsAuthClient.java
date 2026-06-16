package com.iduenduen.mtsservice.domain.ls.auth;

import com.iduenduen.mtsservice.domain.ls.client.LsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class LsAuthClient {

    private final LsProperties lsProperties;
    private final RestClient restClient;

    public LsTokenResponse requestToken() {
        return restClient.post()
                .uri(lsProperties.getBaseUrl() + "/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("grant_type=client_credentials"
                        + "&appkey=" + lsProperties.getAppKey()
                        + "&appsecretkey=" + lsProperties.getAppSecret()
                        + "&scope=oob")
                .retrieve()
                .body(LsTokenResponse.class);
    }
}
