package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtfBackfillAdminService {

    private static final long DELAY_BETWEEN_CALLS_MS = 1000;
    private static final int MINUTE_HISTORY_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EtfRepository etfRepository;
    private final EtfPriceService etfPriceService;
    private final EtfCandleBackfillService etfCandleBackfillService;

    // 아직 등록 안 된 종목만 시딩 (이미 있는 종목은 건드리지 않음)
    public void seedAll() {
        Set<String> existingCodes = etfRepository.findAll().stream()
                .map(Etf::getCode)
                .collect(Collectors.toSet());

        for (String code : SolEtfCodes.CODES) {
            if (existingCodes.contains(code)) {
                continue;
            }
            try {
                etfPriceService.fetchAndSaveCurrentPrice(code);
                log.info("[*] SOL ETF 시딩 완료. code={}", code);
            } catch (Exception e) {
                log.warn("[*] SOL ETF 시딩 실패. code={} - {}", code, e.getMessage());
            }
            sleep();
        }
    }

    // 특정 종목 일/주/월봉 + 분봉(최근 30일) 전부 강제 백필 (스킵 체크 없음, upsert라 안전)
    public void backfillOne(String code) {
        Etf etf = etfRepository.findByCode(code).orElse(null);
        if (etf == null) {
            log.warn("[*] 백필 대상 종목 없음. code={}", code);
            return;
        }

        String today = LocalDate.now().format(DATE_FORMAT);
        String from = LocalDate.now().minusDays(MINUTE_HISTORY_DAYS).format(DATE_FORMAT);

        try {
            etfCandleBackfillService.backfillDaily(etf.getId(), code);
            sleep();
            etfCandleBackfillService.backfillWeekly(etf.getId(), code);
            sleep();
            etfCandleBackfillService.backfillMonthly(etf.getId(), code);
            sleep();
            etfCandleBackfillService.backfillMinute1m(etf.getId(), code, from, today);
            sleep();
            etfCandleBackfillService.backfillMinute10m(etf.getId(), code, from, today);
            sleep();
            etfCandleBackfillService.backfillMinute30m(etf.getId(), code, from, today);
            sleep();
            etfCandleBackfillService.backfillMinute60m(etf.getId(), code, from, today);
            sleep();
            log.info("[*] ETF 백필 완료. code={}", code);
        } catch (Exception e) {
            log.error("[*] ETF 백필 실패. code={}", code, e);
        }
    }

    // 전종목 강제 백필 (백그라운드)
    @Async
    public void backfillAllAsync() {
        log.info("[*] 전종목 백필 시작. 종목수={}", SolEtfCodes.CODES.size());
        for (String code : SolEtfCodes.CODES) {
            backfillOne(code);
        }
        log.info("[*] 전종목 백필 완료.");
    }

    private void sleep() {
        try {
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
