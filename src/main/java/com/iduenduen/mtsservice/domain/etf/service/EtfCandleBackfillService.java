package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedDailyCandleResponse;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedMinuteCandleResponse;
import com.iduenduen.mtsservice.common.ls.stock.service.LsUnifiedDailyCandleService;
import com.iduenduen.mtsservice.common.ls.stock.service.LsUnifiedMinuteCandleService;
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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class EtfCandleBackfillService {

    private static final long MILLION = 1_000_000L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String EXCHANGE_KRX = "K";

    private static final String LISTING_FLOOR_DATE = "19560101";
    private static final int MAX_PAGE = 100;

    private final LsUnifiedDailyCandleService lsUnifiedDailyCandleService;
    private final LsUnifiedMinuteCandleService lsUnifiedMinuteCandleService;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfCandle1wRepository etfCandle1wRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandle10mRepository etfCandle10mRepository;
    private final EtfCandle30mRepository etfCandle30mRepository;
    private final EtfCandle60mRepository etfCandle60mRepository;

    @Transactional
    public void backfillDaily(Long etfId, String shcode) {
        backfillDailyRange(etfId, shcode, LsUnifiedDailyCandleService.GUBUN_DAY, this::upsertDaily);
    }

    @Transactional
    public void backfillWeekly(Long etfId, String shcode) {
        backfillDailyRange(etfId, shcode, LsUnifiedDailyCandleService.GUBUN_WEEK, this::upsertWeekly);
    }

    @Transactional
    public void backfillMinute1m(Long etfId, String shcode, String sdate, String edate) {
        backfillMinuteRange(etfId, shcode, 1, sdate, edate, this::upsertMinute1m);
    }

    @Transactional
    public void backfillMinute10m(Long etfId, String shcode, String sdate, String edate) {
        backfillMinuteRange(etfId, shcode, 10, sdate, edate, this::upsertMinute10m);
    }

    @Transactional
    public void backfillMinute30m(Long etfId, String shcode, String sdate, String edate) {
        backfillMinuteRange(etfId, shcode, 30, sdate, edate, this::upsertMinute30m);
    }

    @Transactional
    public void backfillMinute60m(Long etfId, String shcode, String sdate, String edate) {
        backfillMinuteRange(etfId, shcode, 60, sdate, edate, this::upsertMinute60m);
    }

    private void backfillMinuteRange(Long etfId, String shcode, int ncnt, String sdate, String edate,
                                      BiConsumer<Long, LsUnifiedMinuteCandleResponse.T8452OutBlock1> upsert) {
        LsUnifiedMinuteCandleResponse response =
                lsUnifiedMinuteCandleService.getUnifiedMinuteCandles(shcode, ncnt, sdate, edate, EXCHANGE_KRX);
        applyRows(etfId, response, upsert);

        int page = 0;
        while (response.hasNext() && page < MAX_PAGE) {
            String ctsDate = response.getT8452OutBlock().getCts_date();
            String ctsTime = response.getT8452OutBlock().getCts_time();
            response = lsUnifiedMinuteCandleService.getUnifiedMinuteCandlesContinue(
                    shcode, ncnt, sdate, edate, ctsDate, ctsTime, EXCHANGE_KRX);
            applyRows(etfId, response, upsert);
            page++;
        }
    }

    private void applyRows(Long etfId, LsUnifiedMinuteCandleResponse response,
                            BiConsumer<Long, LsUnifiedMinuteCandleResponse.T8452OutBlock1> upsert) {
        List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> rows = response.getT8452OutBlock1();
        if (rows == null) {
            return;
        }
        for (LsUnifiedMinuteCandleResponse.T8452OutBlock1 row : rows) {
            upsert.accept(etfId, row);
        }
    }

    private void upsertMinute1m(Long etfId, LsUnifiedMinuteCandleResponse.T8452OutBlock1 row) {
        LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle1mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle1mRepository.save(EtfCandle1m.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private void upsertMinute10m(Long etfId, LsUnifiedMinuteCandleResponse.T8452OutBlock1 row) {
        LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle10mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle10mRepository.save(EtfCandle10m.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private void upsertMinute30m(Long etfId, LsUnifiedMinuteCandleResponse.T8452OutBlock1 row) {
        LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle30mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle30mRepository.save(EtfCandle30m.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private void upsertMinute60m(Long etfId, LsUnifiedMinuteCandleResponse.T8452OutBlock1 row) {
        LocalDateTime candleTime = parseCandleDateTime(row.getDate(), row.getTime());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle60mRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle60mRepository.save(EtfCandle60m.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private long parse(String value) {
        return (value == null || value.isBlank()) ? 0L : Long.parseLong(value.trim());
    }

    private LocalDateTime parseCandleDateTime(String yyyymmdd, String time) {
        String timeStr = (time != null && time.length() >= 6) ? time.substring(0, 6) : time;
        return LocalDateTime.parse(yyyymmdd + timeStr, DATETIME_FORMAT);
    }

    @Transactional
    public void backfillMonthly(Long etfId, String shcode) {
        backfillDailyRange(etfId, shcode, LsUnifiedDailyCandleService.GUBUN_MONTH, this::upsertMonthly);
    }

    // 상장일(혹은 그보다 더 이전)부터 오늘까지 전체 구간을 요청해, 거래소 데이터가 존재하는 만큼만 받아온다.
    private void backfillDailyRange(Long etfId, String shcode, String gubun,
                                     BiConsumer<Long, LsUnifiedDailyCandleResponse.T8451OutBlock1> upsert) {
        String today = LocalDate.now().format(DATE_FORMAT);

        LsUnifiedDailyCandleResponse response =
                lsUnifiedDailyCandleService.getUnifiedDailyCandles(shcode, gubun, LISTING_FLOOR_DATE, today, EXCHANGE_KRX);
        applyDailyRows(etfId, response, upsert);

        int page = 0;
        while (response.hasNext() && page < MAX_PAGE) {
            String ctsDate = response.getT8451OutBlock().getCts_date();
            response = lsUnifiedDailyCandleService.getUnifiedDailyCandlesContinue(
                    shcode, gubun, LISTING_FLOOR_DATE, today, ctsDate, EXCHANGE_KRX);
            applyDailyRows(etfId, response, upsert);
            page++;
        }
    }

    private void applyDailyRows(Long etfId, LsUnifiedDailyCandleResponse response,
                                 BiConsumer<Long, LsUnifiedDailyCandleResponse.T8451OutBlock1> upsert) {
        List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows = response.getT8451OutBlock1();
        if (rows == null) {
            return;
        }
        for (LsUnifiedDailyCandleResponse.T8451OutBlock1 row : rows) {
            upsert.accept(etfId, row);
        }
    }

    private void upsertDaily(Long etfId, LsUnifiedDailyCandleResponse.T8451OutBlock1 row) {
        LocalDateTime candleTime = parseDate(row.getDate());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle1dRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle1dRepository.save(EtfCandle1d.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private void upsertWeekly(Long etfId, LsUnifiedDailyCandleResponse.T8451OutBlock1 row) {
        LocalDateTime candleTime = parseDate(row.getDate());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle1wRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle1wRepository.save(EtfCandle1w.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private void upsertMonthly(Long etfId, LsUnifiedDailyCandleResponse.T8451OutBlock1 row) {
        LocalDateTime candleTime = parseDate(row.getDate());
        long open = parse(row.getOpen());
        long high = parse(row.getHigh());
        long low = parse(row.getLow());
        long close = parse(row.getClose());
        long volume = parse(row.getJdiff_vol());
        long tradeAmount = parse(row.getValue()) * MILLION;

        etfCandle1moRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(open, high, low, close, volume, tradeAmount),
                        () -> etfCandle1moRepository.save(EtfCandle1mo.of(etfId, open, high, low, close, volume, tradeAmount, candleTime))
                );
    }

    private LocalDateTime parseDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, DATE_FORMAT).atStartOfDay();
    }
}
