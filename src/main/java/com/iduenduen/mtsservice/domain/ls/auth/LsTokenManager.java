package com.iduenduen.mtsservice.domain.ls.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsTokenManager {

    private final LsAuthClient lsAuthClient;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_KEY = "ls:access-token";

    // 만료 5분 전에 캐시를 비워 미리 갱신되도록 함
    private static final long REFRESH_MARGIN_SECONDS = 300;

    public String getToken() {
        String token = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (token != null) {
            return token;
        }
        return refresh();
    }

    private synchronized String refresh() {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null) {
            return cached;
        }

        log.info("LS 토큰 갱신 중...");
        LsTokenResponse response = lsAuthClient.requestToken();
        String accessToken = response.getAccessToken();
        long ttlSeconds = Math.max(response.getExpiresIn() - REFRESH_MARGIN_SECONDS, 0);

        redisTemplate.opsForValue().set(TOKEN_KEY, accessToken, Duration.ofSeconds(ttlSeconds));
        log.info("LS 토큰 갱신 완료. TTL: {}초", ttlSeconds);
        return accessToken;
    }
}
