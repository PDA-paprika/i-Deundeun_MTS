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

    @Scheduled(cron = "0 55 6 * * *")
    public void refreshBeforeDailyReset() {
        log.info("LS daily token reset scheduled - reconnecting with fresh token.");
        lsRealtimeConnector.reconnect();
    }
}
