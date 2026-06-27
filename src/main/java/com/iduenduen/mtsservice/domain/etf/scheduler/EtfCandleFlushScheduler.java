package com.iduenduen.mtsservice.domain.etf.scheduler;

import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;
import com.iduenduen.mtsservice.domain.etf.service.EtfBackfillAdminService;
import com.iduenduen.mtsservice.domain.etf.service.EtfCandleAccumulatorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.etf.realtime", name = "persist-enabled", havingValue = "true")
@RequiredArgsConstructor
public class EtfCandleFlushScheduler {

    private final EtfCandleAccumulatorService etfCandleAccumulatorService;
    private final EtfBackfillAdminService etfBackfillAdminService;

    // 매분 0초: 1분봉 flush
    @Scheduled(cron = "0 * * * * MON-FRI")
    public void flush1m() {
        SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flush1m);
    }

    // 매 10분: 10분봉 flush
    @Scheduled(cron = "0 0/10 * * * MON-FRI")
    public void flush10m() {
        SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flush10m);
    }

    // 매 30분: 30분봉 flush
    @Scheduled(cron = "0 0/30 * * * MON-FRI")
    public void flush30m() {
        SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flush30m);
    }

    // 매 정시: 60분봉 flush
    @Scheduled(cron = "0 0 * * * MON-FRI")
    public void flush60m() {
        SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flush60m);
    }

    // 장마감(15:30) 직후: 일봉 flush, 이번 달 마지막 영업일이면 월봉도 flush
    @Scheduled(cron = "0 35 15 * * MON-FRI")
    public void flushDaily() {
        SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flushDaily);

        if (isLastTradingDayOfMonth(LocalDate.now())) {
            log.info("[월봉 flush] 이번 달 마지막 영업일 - 월봉 flush 시작");
            SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flushMonthly);
        }
    }

    // 매주 금요일 장마감 직후: 주봉 flush
    @Scheduled(cron = "0 35 15 * * FRI")
    public void flushWeekly() {
        log.info("[주봉 flush] 금요일 장마감 - 주봉 flush 시작");
        SolEtfCodes.CODES.forEach(etfCandleAccumulatorService::flushWeekly);
    }

    // 매 영업일 05:30: 직전 영업일 "하루치" 분봉을 브로커 원본으로 재적재(upsert)해 누락/오염 자동 보정.
    // 실시간 적재가 비거나 어긋난 구간을 다음 날 아침에 권위 있는 데이터로 self-heal 한다.
    // backfillYesterdayAsync는 @Async라 스케줄러 스레드를 막지 않는다.
    @Scheduled(cron = "0 30 5 * * MON-FRI")
    public void backfillCandles() {
        log.info("[백필] 새벽 자동 백필 시작 - 직전 영업일 분봉 보정");
        etfBackfillAdminService.backfillYesterdayAsync();
    }

    private boolean isLastTradingDayOfMonth(LocalDate today) {
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        while (lastDay.getDayOfWeek() == DayOfWeek.SATURDAY || lastDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastDay = lastDay.minusDays(1);
        }
        return today.equals(lastDay);
    }
}
