package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.ls.stock.dto.LsPeriodPriceResponse;
import com.iduenduen.mtsservice.common.ls.stock.service.LsPeriodPriceService;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1mo;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1w;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1moRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1wRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EtfCandleBackfillService {

    private static final long MILLION = 1_000_000L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final int DAY_COUNT = 100;
    private static final int WEEK_COUNT = 52;
    private static final int MONTH_COUNT = 24;

    private final LsPeriodPriceService lsPeriodPriceService;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfCandle1wRepository etfCandle1wRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;

    @Transactional
    public void backfillDaily(Long etfId, String shcode) {
        List<LsPeriodPriceResponse.T1305OutBlock1> rows =
                lsPeriodPriceService.getPeriodPrices(shcode, LsPeriodPriceService.DWM_DAY, DAY_COUNT);
        for (LsPeriodPriceResponse.T1305OutBlock1 row : rows) {
            upsertDaily(etfId, row);
        }
    }

    @Transactional
    public void backfillWeekly(Long etfId, String shcode) {
        List<LsPeriodPriceResponse.T1305OutBlock1> rows =
                lsPeriodPriceService.getPeriodPrices(shcode, LsPeriodPriceService.DWM_WEEK, WEEK_COUNT);
        for (LsPeriodPriceResponse.T1305OutBlock1 row : rows) {
            upsertWeekly(etfId, row);
        }
    }

    @Transactional
    public void backfillMonthly(Long etfId, String shcode) {
        List<LsPeriodPriceResponse.T1305OutBlock1> rows =
                lsPeriodPriceService.getPeriodPrices(shcode, LsPeriodPriceService.DWM_MONTH, MONTH_COUNT);
        for (LsPeriodPriceResponse.T1305OutBlock1 row : rows) {
            upsertMonthly(etfId, row);
        }
    }

    private void upsertDaily(Long etfId, LsPeriodPriceResponse.T1305OutBlock1 row) {
        LocalDateTime candleTime = parseDate(row.getDate());
        etfCandle1dRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(
                                Long.parseLong(row.getOpen()), Long.parseLong(row.getHigh()),
                                Long.parseLong(row.getLow()), Long.parseLong(row.getClose()),
                                Long.parseLong(row.getVolume()), Long.parseLong(row.getValue()) * MILLION
                        ),
                        () -> etfCandle1dRepository.save(EtfCandle1d.of(
                                etfId, Long.parseLong(row.getOpen()), Long.parseLong(row.getHigh()),
                                Long.parseLong(row.getLow()), Long.parseLong(row.getClose()),
                                Long.parseLong(row.getVolume()), Long.parseLong(row.getValue()) * MILLION, candleTime
                        ))
                );
    }

    private void upsertWeekly(Long etfId, LsPeriodPriceResponse.T1305OutBlock1 row) {
        LocalDateTime candleTime = parseDate(row.getDate());
        etfCandle1wRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(
                                Long.parseLong(row.getOpen()), Long.parseLong(row.getHigh()),
                                Long.parseLong(row.getLow()), Long.parseLong(row.getClose()),
                                Long.parseLong(row.getVolume()), Long.parseLong(row.getValue()) * MILLION
                        ),
                        () -> etfCandle1wRepository.save(EtfCandle1w.of(
                                etfId, Long.parseLong(row.getOpen()), Long.parseLong(row.getHigh()),
                                Long.parseLong(row.getLow()), Long.parseLong(row.getClose()),
                                Long.parseLong(row.getVolume()), Long.parseLong(row.getValue()) * MILLION, candleTime
                        ))
                );
    }

    private void upsertMonthly(Long etfId, LsPeriodPriceResponse.T1305OutBlock1 row) {
        LocalDateTime candleTime = parseDate(row.getDate());
        etfCandle1moRepository.findByEtfIdAndCandleTime(etfId, candleTime)
                .ifPresentOrElse(
                        existing -> existing.updateSnapshot(
                                Long.parseLong(row.getOpen()), Long.parseLong(row.getHigh()),
                                Long.parseLong(row.getLow()), Long.parseLong(row.getClose()),
                                Long.parseLong(row.getVolume()), Long.parseLong(row.getValue()) * MILLION
                        ),
                        () -> etfCandle1moRepository.save(EtfCandle1mo.of(
                                etfId, Long.parseLong(row.getOpen()), Long.parseLong(row.getHigh()),
                                Long.parseLong(row.getLow()), Long.parseLong(row.getClose()),
                                Long.parseLong(row.getVolume()), Long.parseLong(row.getValue()) * MILLION, candleTime
                        ))
                );
    }

    private LocalDateTime parseDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, DATE_FORMAT).atStartOfDay();
    }
}
