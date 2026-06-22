package com.iduenduen.mtsservice.domain.etf.scheduler;

import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;
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

    private boolean isLastTradingDayOfMonth(LocalDate today) {
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        while (lastDay.getDayOfWeek() == DayOfWeek.SATURDAY || lastDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastDay = lastDay.minusDays(1);
        }
        return today.equals(lastDay);
    }
}
