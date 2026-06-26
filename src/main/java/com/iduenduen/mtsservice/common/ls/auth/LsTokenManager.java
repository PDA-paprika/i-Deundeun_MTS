package com.iduenduen.mtsservice.common.ls.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsTokenManager {

    private static final String TOKEN_KEY = "ls:access-token";
    private static final long REFRESH_MARGIN_SECONDS = 300;

    private final LsAuthClient lsAuthClient;
    private final StringRedisTemplate redisTemplate;

    public String getToken() {
        String token = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (token != null) {
            return token;
        }
        return refresh();
    }

    public void clearToken() {
        redisTemplate.delete(TOKEN_KEY);
        log.info("LS access token cache cleared.");
    }

    private synchronized String refresh() {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null) {
            return cached;
        }

        log.info("Refreshing LS access token.");
        LsTokenResponse response = lsAuthClient.requestToken();
        String accessToken = response.getAccessToken();
        long ttlSeconds = Math.max(response.getExpiresIn() - REFRESH_MARGIN_SECONDS, 0);

        redisTemplate.opsForValue().set(TOKEN_KEY, accessToken, Duration.ofSeconds(ttlSeconds));
        log.info("LS access token refreshed. ttlSeconds={}", ttlSeconds);
        return accessToken;
    }
}
