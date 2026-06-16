package com.iduenduen.mtsservice.domain.ls.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsTokenManager {

    private final LsAuthClient lsAuthClient;

    private String accessToken;
    private Instant expiresAt;

    // 만료 5분 전부터 미리 갱신
    private static final long REFRESH_MARGIN_SECONDS = 300;

    public synchronized String getToken() {
        if (isTokenExpired()) {
            refresh();
        }
        return accessToken;
    }

    private boolean isTokenExpired() {
        return accessToken == null
                || expiresAt == null
                || Instant.now().isAfter(expiresAt.minusSeconds(REFRESH_MARGIN_SECONDS));
    }

    private void refresh() {
        log.info("LS 토큰 갱신 중...");
        LsTokenResponse response = lsAuthClient.requestToken();
        accessToken = response.getAccessToken();
        expiresAt = Instant.now().plusSeconds(response.getExpiresIn());
        log.info("LS 토큰 갱신 완료. 만료: {}", expiresAt);
    }
}
