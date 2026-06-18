package com.iduenduen.mtsservice.domain.etf.seed;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1moRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.service.EtfCandleBackfillService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.etf.auto-collect", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class EtfCandleBackfillRunner implements ApplicationRunner {

    private static final long DELAY_BETWEEN_CALLS_MS = 1000;

    private final EtfRepository etfRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;
    private final EtfCandleBackfillService etfCandleBackfillService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (Etf etf : etfRepository.findAll()) {
            if (etfCandle1moRepository.existsByEtfId(etf.getId())) {
                continue;
            }
            backfillOne(etf);
        }
    }

    private void backfillOne(Etf etf) {
        try {
            etfCandleBackfillService.backfillDaily(etf.getId(), etf.getCode());
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            etfCandleBackfillService.backfillWeekly(etf.getId(), etf.getCode());
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            etfCandleBackfillService.backfillMonthly(etf.getId(), etf.getCode());
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            log.info("[*] ETF 캔들 백필 완료. code={}", etf.getCode());
        } catch (Exception e) {
            log.error("[*] ETF 캔들 백필 실패. code={}", etf.getCode(), e);
        }
    }
}
