package com.iduenduen.mtsservice.domain.ls.auth;

import com.iduenduen.mtsservice.common.ls.auth.LsAuthClient;
import com.iduenduen.mtsservice.common.ls.auth.LsTokenResponse;
import com.iduenduen.mtsservice.common.ls.client.LsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class LsAuthClientTest {

    @Test
    void requestToken_returnsAccessToken() {
        LsProperties properties = new LsProperties();
        properties.setBaseUrl("https://openapi.ls-sec.co.kr:8080");
        properties.setAppKey(System.getProperty("ls.appKey", "여기에 발급받은 appkey 입력"));
        properties.setAppSecret(System.getProperty("ls.appSecret", "여기에 발급받은 appsecretkey 입력"));

        LsAuthClient lsAuthClient = new LsAuthClient(properties, RestClient.create());

        LsTokenResponse response;
        try {
            response = lsAuthClient.requestToken();
        } catch (RestClientResponseException e) {
            fail("LS 토큰 발급 실패 (status=" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return;
        }

        System.out.println("access_token = " + response.getAccessToken());
        System.out.println("expires_in   = " + response.getExpiresIn());

        assertFalse(response.getAccessToken() == null || response.getAccessToken().isBlank());
    }
}
