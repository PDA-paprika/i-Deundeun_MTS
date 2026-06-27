package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedDailyCandleResponse;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedMinuteCandleResponse;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle10m;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1mo;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1w;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle30m;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle60m;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle10mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1moRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1wRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle30mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle60mRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 백필로 수집한 캔들 row를 DB에 저장(upsert)하는 전용 서비스.
 * HTTP 수집(EtfCandleBackfillService)과 트랜잭션을 분리해, 트랜잭션이 외부 API 호출 시간 동안
 * 커넥션을 점유하지 않도록 한다. 대량 row는 BATCH_SIZE 단위로 flush/clear 해서 영속성 컨텍스트가
 * 비대해지는 것을 막는다.
 */
@Service
@RequiredArgsConstructor
public class EtfCandlePersistService {

    private static final long MILLION = 1_000_000L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int BATCH_SIZE = 500;

    // 봉은 "끝시각" 라벨. 정규장 분봉만 저장(장전/시간외 제외): 1/10/30분봉=09:00~15:30, 60분봉=09:00~16:00.
    private static final LocalTime SESSION_OPEN = LocalTime.of(9, 0);
    private static final LocalTime SESSION_CLOSE = LocalTime.of(15, 30);
    private static final LocalTime SESSION_CLOSE_60M = LocalTime.of(16, 0);

    // 정규장 시간 밖(장전/시간외) 분봉이면 true → 저장 스킵.
    private boolean outOfSession(LocalDateTime candleTime, LocalTime close) {
        LocalTime t = candleTime.toLocalTime();
        return t.isBefore(SESSION_OPEN) || t.isAfter(close);
    }

    @PersistenceContext
    private EntityManager entityManager;

    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfCandle1wRepository etfCandle1wRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandle10mRepository etfCandle10mRepository;
    private final EtfCandle30mRepository etfCandle30mRepository;
    private final EtfCandle60mRepository etfCandle60mRepository;

    @Transactional
    public void saveDaily(Long etfId, List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows) {
        int i = 0;
        for (LsUnifiedDailyCandleResponse.T8451OutBlock1 row : rows) {
            LocalDateTime candleTime = parseDate(row.getDate());
            etfCandle1dRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle1dRepository.save(EtfCandle1d.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    @Transactional
    public void saveWeekly(Long etfId, List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows) {
        // 진행 중인 주는 백필 때마다 그날 날짜로 새 행이 쌓여 중복이 생기므로, 완료된 주(이번 주 월요일 이전)만 저장한다.
        LocalDate currentWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        int i = 0;
        for (LsUnifiedDailyCandleResponse.T8451OutBlock1 row : rows) {
            LocalDateTime candleTime = parseDate(row.getDate());
            if (!candleTime.toLocalDate().isBefore(currentWeekMonday)) continue;
            etfCandle1wRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle1wRepository.save(EtfCandle1w.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    @Transactional
    public void saveMonthly(Long etfId, List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows) {
        // 진행 중인 달은 백필 때마다 그날 날짜로 새 행이 쌓여 중복이 생기므로, 완료된 달(이번 달 1일 이전)만 저장한다.
        LocalDate currentMonthFirst = LocalDate.now().withDayOfMonth(1);
        int i = 0;
        for (LsUnifiedDailyCandleResponse.T8451OutBlock1 row : rows) {
            LocalDateTime candleTime = parseDate(row.getDate());
            if (!candleTime.toLocalDate().isBefore(currentMonthFirst)) continue;
            etfCandle1moRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle1moRepository.save(EtfCandle1mo.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    @Transactional
    public void saveMinute1m(Long etfId, List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> rows) {
        int i = 0;
        for (LsUnifiedMinuteCandleResponse.T8452OutBlock1 row : rows) {
            LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
            if (outOfSession(candleTime, SESSION_CLOSE)) continue;
            etfCandle1mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle1mRepository.save(EtfCandle1m.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    @Transactional
    public void saveMinute10m(Long etfId, List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> rows) {
        int i = 0;
        for (LsUnifiedMinuteCandleResponse.T8452OutBlock1 row : rows) {
            LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
            if (outOfSession(candleTime, SESSION_CLOSE)) continue;
            etfCandle10mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle10mRepository.save(EtfCandle10m.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    @Transactional
    public void saveMinute30m(Long etfId, List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> rows) {
        int i = 0;
        for (LsUnifiedMinuteCandleResponse.T8452OutBlock1 row : rows) {
            LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
            if (outOfSession(candleTime, SESSION_CLOSE)) continue;
            etfCandle30mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle30mRepository.save(EtfCandle30m.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    @Transactional
    public void saveMinute60m(Long etfId, List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> rows) {
        int i = 0;
        for (LsUnifiedMinuteCandleResponse.T8452OutBlock1 row : rows) {
            LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
            if (outOfSession(candleTime, SESSION_CLOSE_60M)) continue;
            etfCandle60mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                    .ifPresentOrElse(
                            existing -> existing.updateSnapshot(parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION),
                            () -> etfCandle60mRepository.save(EtfCandle60m.of(etfId, parse(row.getOpen()), parse(row.getHigh()),
                                    parse(row.getLow()), parse(row.getClose()), parse(row.getJdiff_vol()), parse(row.getValue()) * MILLION, candleTime))
                    );
            flushIfNeeded(++i);
        }
    }

    private void flushIfNeeded(int processed) {
        if (processed % BATCH_SIZE == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

    private long parse(String value) {
        return (value == null || value.isBlank()) ? 0L : Long.parseLong(value.trim());
    }

    private LocalDateTime parseDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, DATE_FORMAT).atStartOfDay();
    }

    private LocalDateTime parseCandleDateTime(String yyyymmdd, String time) {
        String timeStr = (time != null && time.length() >= 6) ? time.substring(0, 6) : time;
        return LocalDateTime.parse(yyyymmdd + timeStr, DATETIME_FORMAT);
    }
}
