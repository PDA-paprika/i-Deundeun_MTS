package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;
import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
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
    private final EtfCandle1mRepository etfCandle1mRepository;

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

    // 전종목 "일봉만" 백필 (백그라운드). 분봉(90일×4종)을 건너뛰어 훨씬 빠르다.
    // 거래대금 0으로 굳은 기존 일봉을 t8451 값으로 upsert 보정하는 용도.
    @Async
    public void backfillDailyAllAsync() {
        log.info("[*] 전종목 일봉 백필 시작(거래대금 보정). 종목수={}", SolEtfCodes.CODES.size());
        int processed = 0;
        for (String code : SolEtfCodes.CODES) {
            Etf etf = etfRepository.findByCode(code).orElse(null);
            if (etf == null) {
                log.warn("[*] 백필 대상 종목 없음. code={}", code);
                continue;
            }
            Long etfId = etf.getId();
            runStep(code, "일봉", () -> etfCandleBackfillService.backfillDaily(etfId, code));
            processed++;
        }
        log.info("[*] 전종목 일봉 백필 완료. 처리={}", processed);
    }

    // 직전 영업일 "하루치"만 전종목 분봉(1/10/30/60m) 재적재(upsert). 새벽 self-heal용 경량 백필.
    // 일/주/월봉은 실시간 flush와 경계 flush가 채우므로 야간 보정에서는 제외한다.
    @Async
    public void backfillYesterdayAsync() {
        String day = lastTradingDay(LocalDate.now()).format(DATE_FORMAT);
        log.info("[*] 직전 영업일 분봉 백필 시작. 대상일={}, 종목수={}", day, SolEtfCodes.CODES.size());
        for (String code : SolEtfCodes.CODES) {
            backfillYesterdayMinutes(code, day);
        }
        log.info("[*] 직전 영업일 분봉 백필 완료. 대상일={}", day);
    }

    // 한 종목의 직전 영업일 분봉만 보정 (sdate=edate=day → 하루치).
    private void backfillYesterdayMinutes(String code, String day) {
        Etf etf = etfRepository.findByCode(code).orElse(null);
        if (etf == null) {
            log.warn("[*] 백필 대상 종목 없음. code={}", code);
            return;
        }
        Long etfId = etf.getId();

        int failed = 0;
        failed += runStep(code, "1분봉", () -> etfCandleBackfillService.backfillMinute1m(etfId, code, day, day));
        failed += runStep(code, "10분봉", () -> etfCandleBackfillService.backfillMinute10m(etfId, code, day, day));
        failed += runStep(code, "30분봉", () -> etfCandleBackfillService.backfillMinute30m(etfId, code, day, day));
        failed += runStep(code, "60분봉", () -> etfCandleBackfillService.backfillMinute60m(etfId, code, day, day));

        if (failed > 0) {
            log.warn("[*] 직전 영업일 분봉 백필 일부 실패. code={}, 실패단계수={}", code, failed);
        }
    }

    // 직전 영업일 계산 (주말 스킵). 월요일 새벽이면 금요일을 가리킨다.
    private LocalDate lastTradingDay(LocalDate date) {
        LocalDate prev = date.minusDays(1);
        if (prev.getDayOfWeek() == DayOfWeek.SUNDAY)   return prev.minusDays(2);
        if (prev.getDayOfWeek() == DayOfWeek.SATURDAY) return prev.minusDays(1);
        return prev;
    }

    // 분봉 백필이 "이미 됐다"고 판정하는 기준일. 실시간 적재만 된 종목(최근 1~2일치)과
    // 백필된 종목(약 90일치)을 확실히 구분하기 위해, 목표 범위(90일)보다 마진(7일)을 둔다.
    private static final int FILLED_THRESHOLD_DAYS = MINUTE_HISTORY_DAYS - 7;

    // 아직 90일치 분봉이 안 채워진 종목만 골라서 백필 (백그라운드).
    // 호출 제한 등으로 중간에 실패해도 다시 호출하면 빠진 종목만 이어서 채울 수 있다.
    @Async
    public void backfillMissingAsync() {
        LocalDate threshold = LocalDate.now().minusDays(FILLED_THRESHOLD_DAYS);
        log.info("[*] 미완 종목 백필 시작. 채움기준={} 이전 데이터 존재 여부", threshold);

        int skipped = 0;
        int processed = 0;
        for (String code : SolEtfCodes.CODES) {
            Etf etf = etfRepository.findByCode(code).orElse(null);
            if (etf != null && isMinuteFilled(etf.getId(), threshold)) {
                skipped++;
                continue;
            }
            backfillOne(code);
            processed++;
        }
        log.info("[*] 미완 종목 백필 완료. 처리={}, 스킵(이미완료)={}", processed, skipped);
    }

    // threshold 이전의 1분봉이 하나라도 있으면 = 과거 백필이 된 종목으로 간주.
    private boolean isMinuteFilled(Long etfId, LocalDate threshold) {
        return etfCandle1mRepository.existsByEtfIdAndCandleTimeLessThanEqual(
                etfId, threshold.atStartOfDay());
    }

    private void sleep() {
        try {
            Thread.sleep(DELAY_BETWEEN_CALLS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
