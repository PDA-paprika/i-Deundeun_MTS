package com.iduenduen.mtsservice.domain.etf.seed;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.service.EtfCandleBackfillService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@Order(3)
@ConditionalOnProperty(prefix = "app.etf.auto-collect", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class EtfMinuteCandleBackfillRunner implements ApplicationRunner {

    private static final long DELAY_BETWEEN_CALLS_MS = 1000;
    private static final int MINUTE_HISTORY_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EtfRepository etfRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandleBackfillService etfCandleBackfillService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String today = LocalDate.now().format(DATE_FORMAT);
        String from = LocalDate.now().minusDays(MINUTE_HISTORY_DAYS).format(DATE_FORMAT);

        for (Etf etf : etfRepository.findAll()) {
            if (etfCandle1mRepository.existsByEtfId(etf.getId())) {
                continue;
            }
            backfillOne(etf, from, today);
        }
    }

    private void backfillOne(Etf etf, String sdate, String edate) {
        try {
            etfCandleBackfillService.backfillMinute1m(etf.getId(), etf.getCode(), sdate, edate);
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            etfCandleBackfillService.backfillMinute10m(etf.getId(), etf.getCode(), sdate, edate);
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            etfCandleBackfillService.backfillMinute30m(etf.getId(), etf.getCode(), sdate, edate);
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            etfCandleBackfillService.backfillMinute60m(etf.getId(), etf.getCode(), sdate, edate);
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
            log.info("[*] ETF 분봉 백필 완료. code={}, range={}~{}", etf.getCode(), sdate, edate);
        } catch (Exception e) {
            log.error("[*] ETF 분봉 백필 실패. code={}", etf.getCode(), e);
        }
    }
}
