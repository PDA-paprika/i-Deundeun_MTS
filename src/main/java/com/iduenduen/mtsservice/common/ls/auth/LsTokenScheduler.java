package com.iduenduen.mtsservice.common.ls.auth;

import com.iduenduen.mtsservice.common.ls.realtime.LsRealtimeConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.etf.realtime", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LsTokenScheduler {

    private final LsRealtimeConnector lsRealtimeConnector;

    // LS 토큰은 매일 07:00에 초기화된다. 초기화 이후(07:05)에 재연결해 새 토큰으로 구독해야
    // 장 시작(09:00) 시점에 유효한 토큰으로 실시간 시세가 들어온다.
    // (초기화 전에 받은 토큰은 07:00에 무효화되어 장 시작 시 가격이 안 옴)
    @Scheduled(cron = "0 5 7 * * *")
    public void reconnectAfterDailyTokenReset() {
        log.info("LS daily token reset passed - reconnecting with fresh token.");
        lsRealtimeConnector.reconnect();
    }
}
