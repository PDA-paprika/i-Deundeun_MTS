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
    private static final int MINUTE_HISTORY_DAYS = 90;
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

    // 특정 종목 일/주/월봉(상장일 전체) + 분봉(최근 MINUTE_HISTORY_DAYS일) 강제 백필 (스킵 체크 없음, upsert라 안전)
    public void backfillOne(String code) {
        Etf etf = etfRepository.findByCode(code).orElse(null);
        if (etf == null) {
            log.warn("[*] 백필 대상 종목 없음. code={}", code);
            return;
        }
        Long etfId = etf.getId();

        // 오늘 분봉은 실시간 적재가 채우는 중이라 동시 쓰기 충돌이 나므로, 어제까지만 백필한다.
        String until = LocalDate.now().minusDays(1).format(DATE_FORMAT);
        String from = LocalDate.now().minusDays(MINUTE_HISTORY_DAYS).format(DATE_FORMAT);

        // 단계별로 독립 실행한다. 한 단계(예: 특정 분봉)가 실패해도 나머지 단계는 계속 진행되도록 분리.
        int failed = 0;
        failed += runStep(code, "일봉", () -> etfCandleBackfillService.backfillDaily(etfId, code));
        failed += runStep(code, "주봉", () -> etfCandleBackfillService.backfillWeekly(etfId, code));
        failed += runStep(code, "월봉", () -> etfCandleBackfillService.backfillMonthly(etfId, code));
        failed += runStep(code, "1분봉", () -> etfCandleBackfillService.backfillMinute1m(etfId, code, from, until));
        failed += runStep(code, "10분봉", () -> etfCandleBackfillService.backfillMinute10m(etfId, code, from, until));
        failed += runStep(code, "30분봉", () -> etfCandleBackfillService.backfillMinute30m(etfId, code, from, until));
        failed += runStep(code, "60분봉", () -> etfCandleBackfillService.backfillMinute60m(etfId, code, from, until));

        if (failed == 0) {
            log.info("[*] ETF 백필 완료. code={}", code);
        } else {
            log.warn("[*] ETF 백필 완료(일부 실패). code={}, 실패단계수={}", code, failed);
        }
    }

    // 단계 하나를 실행하고, 실패 시 로그만 남긴 뒤 1을 반환(다음 단계 진행). 성공 시 0.
    private int runStep(String code, String label, Runnable step) {
        try {
            step.run();
            return 0;
        } catch (Exception e) {
            log.error("[*] ETF 백필 단계 실패. code={}, 단계={} - {}", code, label, e.getMessage());
            return 1;
        } finally {
            sleep();
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
