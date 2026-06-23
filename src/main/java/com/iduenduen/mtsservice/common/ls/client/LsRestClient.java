package com.iduenduen.mtsservice.common.ls.client;

import com.iduenduen.mtsservice.common.ls.auth.LsTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class LsRestClient {

    // LS API 초당 호출 제한에 걸렸을 때 재시도 정책
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 3000;

    private final RestClient restClient;
    private final LsProperties lsProperties;
    private final LsTokenManager lsTokenManager;

    public <T> T post(String path, String trCd, Object requestBody, Class<T> responseType) {
        return post(path, trCd, requestBody, responseType, "N", "");
    }

    public <T> T post(String path, String trCd, Object requestBody, Class<T> responseType, String trCont, String trContKey) {
        return executeWithRetry(trCd, () -> restClient.post()
                .uri(lsProperties.getBaseUrl() + path)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + lsTokenManager.getToken())
                .header("tr_cd", trCd)
                .header("tr_cont", trCont)
                .header("tr_cont_key", trContKey == null ? "" : trContKey)
                .header("mac_address", lsProperties.getMacAddress() == null ? "" : lsProperties.getMacAddress())
                .body(requestBody)
                .retrieve()
                .body(responseType));
    }

    // 호출 제한(429 또는 LS rate-limit 응답)이면 백오프 후 재시도, 그 외 에러는 그대로 던진다.
    private <T> T executeWithRetry(String trCd, Supplier<T> call) {
        int attempt = 0;
        while (true) {
            try {
                return call.get();
            } catch (HttpStatusCodeException e) {
                if (!isRateLimited(e) || attempt >= MAX_RETRIES) {
                    throw e;
                }
                attempt++;
                long backoff = BASE_BACKOFF_MS * attempt;
                log.warn("LS 호출 제한 감지. trCd={}, {}ms 후 재시도 ({}/{}), status={}, body={}",
                        trCd, backoff, attempt, MAX_RETRIES, e.getStatusCode().value(), e.getResponseBodyAsString());
                sleep(backoff);
            }
        }
    }

    private boolean isRateLimited(HttpStatusCodeException e) {
        if (e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return true;
        }
        String body = e.getResponseBodyAsString();
        if (body == null) {
            return false;
        }
        // LS는 초당 호출 한도 초과를 IGW00201 등의 코드/메시지로 내려준다.
        return body.contains("IGW00201") || body.contains("초당") || body.contains("호출 횟수");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
