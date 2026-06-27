package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.dto.ChartCandle;
import com.iduenduen.mtsservice.domain.etf.dto.EtfChartResponse;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle10mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1moRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1wRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle30mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle60mRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfChartService {

    private static final Set<String> SUPPORTED_INTERVALS = Set.of("1m", "10m", "30m", "60m", "1d", "1w", "1mo");
    private static final LocalDateTime DEFAULT_FROM = LocalDateTime.of(2000, 1, 1, 0, 0);

    // 분봉 계열만 정규장 시간 필터를 적용한다. 일/주/월봉은 candleTime이 00:00이라 제외.
    // 봉 시각은 "끝시각" 라벨이라 정규장 마지막 봉은 1/10/30분봉=15:30, 60분봉=16:00(15:00~16:00 구간).
    private static final Set<String> MINUTE_INTERVALS = Set.of("1m", "10m", "30m", "60m");
    private static final LocalTime SESSION_OPEN = LocalTime.of(9, 0);
    private static final LocalTime SESSION_CLOSE = LocalTime.of(15, 30);
    private static final LocalTime SESSION_CLOSE_60M = LocalTime.of(16, 0);

    private final EtfRepository etfRepository;
    private final EtfCandle1mRepository etfCandle1mRepository;
    private final EtfCandle10mRepository etfCandle10mRepository;
    private final EtfCandle30mRepository etfCandle30mRepository;
    private final EtfCandle60mRepository etfCandle60mRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfCandle1wRepository etfCandle1wRepository;
    private final EtfCandle1moRepository etfCandle1moRepository;
    private final EtfCandleAccumulatorService etfCandleAccumulatorService;

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
            case "10m" -> etfCandle10mRepository
                    .findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(etf.getId(), rangeFrom, rangeTo, pageable)
                    .stream()
                    .map(c -> ChartCandle.of(c.getCandleTime(), c.getOpenPrice(), c.getHighPrice(), c.getLowPrice(), c.getClosePrice(), c.getVolume()))
                    .toList();
            case "30m" -> etfCandle30mRepository
                    .findByEtfIdAndCandleTimeBetweenOrderByCandleTimeDesc(etf.getId(), rangeFrom, rangeTo, pageable)
                    .stream()
                    .map(c -> ChartCandle.of(c.getCandleTime(), c.getOpenPrice(), c.getHighPrice(), c.getLowPrice(), c.getClosePrice(), c.getVolume()))
                    .toList();
            case "60m" -> etfCandle60mRepository
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

        // 정규장(09:00~15:30) 외 데이터 제외 + 60분봉 마감봉(15:30) 표기 보정. 라이브 캔들 병합 전에 적용.
        candles = applySessionRules(interval, candles);

        // to가 명시적으로 과거로 지정된 조회(페이징/히스토리)는 진행 중인 캔들을 끼워넣지 않음
        if (to == null) {
            candles = withLiveCandle(candles, etfCode, interval);
        }

        return EtfChartResponse.of(etfCode, interval, candles);
    }

    // 분봉 계열만: (1) 정규장 밖(장전/시간외, 예: 15:50·18:00) 봉 제외, (2) 60분봉의 이상치 15:30 라벨을 16:00으로 보정.
    // 60분봉은 정규장 마지막 봉이 16:00이므로 16:00까지 허용하고, 그 외 분봉은 15:30까지만 허용한다.
    private List<ChartCandle> applySessionRules(String interval, List<ChartCandle> candles) {
        if (!MINUTE_INTERVALS.contains(interval)) {
            return candles;
        }
        boolean is60m = "60m".equals(interval);
        LocalTime close = is60m ? SESSION_CLOSE_60M : SESSION_CLOSE;
        List<ChartCandle> result = new ArrayList<>(candles.size());
        for (ChartCandle c : candles) {
            LocalTime t = c.getCandleTime().toLocalTime();
            if (t.isBefore(SESSION_OPEN) || t.isAfter(close)) {
                continue;
            }
            if (is60m && t.equals(SESSION_CLOSE)) {
                // 드물게 15:30으로 찍힌 60분봉 마감봉을 정상 컨벤션(16:00)으로 통일
                result.add(ChartCandle.of(
                        c.getCandleTime().toLocalDate().atTime(16, 0),
                        c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()));
            } else {
                result.add(c);
            }
        }
        return result;
    }

    private List<ChartCandle> withLiveCandle(List<ChartCandle> candles, String code, String interval) {
        LocalDateTime liveCandleTime = switch (interval) {
            case "1m" -> LocalDateTime.now().withSecond(0).withNano(0);
            case "10m" -> floorMinutes(10);
            case "30m" -> floorMinutes(30);
            case "60m" -> floorMinutes(60);
            case "1d" -> LocalDate.now().atStartOfDay();
            default -> null; // 1w, 1mo는 금요일/말일 flush 1회로 충분, 라이브 병합 생략
        };
        if (liveCandleTime == null) {
            return candles;
        }

        String keyPrefix = switch (interval) {
            case "1m" -> etfCandleAccumulatorService.key1m();
            case "10m" -> etfCandleAccumulatorService.key10m();
            case "30m" -> etfCandleAccumulatorService.key30m();
            case "60m" -> etfCandleAccumulatorService.key60m();
            case "1d" -> etfCandleAccumulatorService.key1d();
            default -> null;
        };

        Map<Object, Object> live = etfCandleAccumulatorService.getCurrentCandle(code, keyPrefix);
        if (live.isEmpty() || live.get("open") == null) {
            return candles;
        }

        ChartCandle liveCandle = ChartCandle.of(
                liveCandleTime,
                Long.parseLong((String) live.get("open")),
                Long.parseLong((String) live.get("high")),
                Long.parseLong((String) live.get("low")),
                Long.parseLong((String) live.get("close")),
                Long.parseLong((String) live.get("volume"))
        );

        List<ChartCandle> result = new ArrayList<>(candles);
        result.removeIf(c -> c.getCandleTime().equals(liveCandleTime));
        result.add(0, liveCandle);
        return result;
    }

    private LocalDateTime floorMinutes(int bucketMinutes) {
        LocalDateTime now = LocalDateTime.now();
        int floored = now.getMinute() - (now.getMinute() % bucketMinutes);
        return now.withMinute(floored).withSecond(0).withNano(0);
    }
}
