package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.dto.ChartCandle;
import com.iduenduen.mtsservice.domain.etf.dto.EtfChartResponse;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1moRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1wRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfChartService {

    private static final Set<String> SUPPORTED_INTERVALS = Set.of("1m", "1d", "1w", "1mo");
    private static final LocalDateTime DEFAULT_FROM = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final EtfRepository etfRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfCandle1wRepository etfCandle1wRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;

    public EtfChartResponse getChart(String etfCode, String interval, LocalDateTime from, LocalDateTime to, int limit) {
        if (!SUPPORTED_INTERVALS.contains(interval)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        Etf etf = etfRepository.findByCode(etfCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        LocalDateTime rangeFrom = from != null ? from : DEFAULT_FROM;
        LocalDateTime rangeTo = to != null ? to : LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);

        List<ChartCandle> candles = switch (interval) {
            case "1m" -> etfCandle1mRepository
                    .findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(etf.getId(), rangeFrom, rangeTo, pageable)
                    .stream()
                    .map(c -> ChartCandle.of(c.getCandleTime(), c.getOpenPrice(), c.getHighPrice(), c.getLowPrice(), c.getClosePrice(), c.getVolume()))
                    .toList();
            case "1d" -> etfCandle1dRepository
                    .findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(etf.getId(), rangeFrom, rangeTo, pageable)
                    .stream()
                    .map(c -> ChartCandle.of(c.getCandleTime(), c.getOpenPrice(), c.getHighPrice(), c.getLowPrice(), c.getClosePrice(), c.getVolume()))
                    .toList();
            case "1w" -> etfCandle1wRepository
                    .findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(etf.getId(), rangeFrom, rangeTo, pageable)
                    .stream()
                    .map(c -> ChartCandle.of(c.getCandleTime(), c.getOpenPrice(), c.getHighPrice(), c.getLowPrice(), c.getClosePrice(), c.getVolume()))
                    .toList();
            case "1mo" -> etfCandle1moRepository
                    .findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(etf.getId(), rangeFrom, rangeTo, pageable)
                    .stream()
                    .map(c -> ChartCandle.of(c.getCandleTime(), c.getOpenPrice(), c.getHighPrice(), c.getLowPrice(), c.getClosePrice(), c.getVolume()))
                    .toList();
            default -> throw new GeneralException(ErrorStatus.BAD_REQUEST);
        };

        return EtfChartResponse.of(etfCode, interval, candles);
    }
}
