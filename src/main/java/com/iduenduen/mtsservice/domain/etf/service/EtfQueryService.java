package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfDetailResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfListItem;
import com.iduenduen.mtsservice.domain.etf.dto.EtfListResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfValuationAverageResponse;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfQueryService {

    private final EtfRepository etfRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfRealtimeCacheService etfRealtimeCacheService;

    public EtfDetailResponse getEtfDetail(String code) {
        Etf etf = etfRepository.findByCode(code)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        List<EtfCandle1d> latestTwo = etfCandle1dRepository.findTop2ByEtfIdOrderByCandleTimeDesc(etf.getId());
        if (latestTwo.isEmpty()) {
            throw new GeneralException(ErrorStatus.ETF_NOT_FOUND);
        }

        EtfCandle1d today = latestTwo.get(0);
        LiveQuote quote = resolveQuote(code, today, latestTwo);

        return EtfDetailResponse.of(
                etf, quote.currentPrice(), quote.priceChange(), quote.changeRate(),
                today.getHighPrice(), today.getLowPrice(), quote.volume(), quote.tradeAmount()
        );
    }

    public EtfListResponse getEtfList(String sort, String q, int page, int limit) {
        List<Etf> etfs = (q == null || q.isBlank())
                ? etfRepository.findAll()
                : etfRepository.findByNameContainingIgnoreCase(q);

        return buildListResponse(etfs, sort, page, limit);
    }

    public EtfListResponse searchEtfs(String keyword, int page, int limit) {
        if (keyword == null || keyword.isBlank()) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        List<Etf> etfs = etfRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword);
        return buildListResponse(etfs, "name", page, limit);
    }

    public EtfValuationAverageResponse getValuationAverage(String code, LocalDate from, LocalDate to) {
        Etf etf = etfRepository.findByCode(code)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        Double avg = etfCandle1dRepository.findAverageClosePriceByEtfIdAndPeriod(
                etf.getId(),
                from.atStartOfDay(),
                to.atTime(LocalTime.MAX));

        long averagePrice = avg != null ? Math.round(avg) : 0L;

        return EtfValuationAverageResponse.builder()
                .etfCode(code)
                .from(from)
                .to(to)
                .averagePrice(averagePrice)
                .build();
    }

    public EtfDetailResponse getEtfDetailById(Long etfId) {
        Etf etf = etfRepository.findById(etfId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));
        return getEtfDetail(etf.getCode());
    }

    private EtfListResponse buildListResponse(List<Etf> etfs, String sort, int page, int limit) {
        int normalizedPage = Math.max(page, 0);
        int normalizedLimit = Math.max(limit, 1);

        // 배치 조회로 N+1 제거: 일봉은 1쿼리, 시세 캐시는 MGET 1번
        Map<Long, List<EtfCandle1d>> dailyByEtfId = loadRecentDailyCandles(
                etfs.stream().map(Etf::getId).toList());
        Map<String, EtfRealtimeCacheService.PriceSnapshot> cacheByCode =
                etfRealtimeCacheService.getCachedPrices(etfs.stream().map(Etf::getCode).toList());

        List<RankedItem> rankedItems = etfs.stream()
                .map(etf -> toRankedItem(etf, dailyByEtfId.get(etf.getId()), cacheByCode.get(etf.getCode())))
                .filter(item -> item != null)
                .sorted(comparatorFor(sort))
                .toList();

        long totalCount = rankedItems.size();
        int fromIndex = Math.min(normalizedPage * normalizedLimit, rankedItems.size());
        int toIndex = Math.min(fromIndex + normalizedLimit, rankedItems.size());

        List<EtfListItem> pageItems = rankedItems.subList(fromIndex, toIndex).stream()
                .map(RankedItem::listItem)
                .toList();

        return EtfListResponse.of(totalCount, pageItems);
    }

    // 전 종목 최근 일봉을 1쿼리로 가져와 etfId별 최신 2개로 그룹핑한다. (종목별 findTop2 N+1 제거)
    private Map<Long, List<EtfCandle1d>> loadRecentDailyCandles(List<Long> etfIds) {
        if (etfIds.isEmpty()) {
            return Map.of();
        }
        // 최신 2개 거래일을 확보할 만큼 넉넉한 윈도우(휴장 포함). 일봉이라 행 수도 적다.
        LocalDateTime windowStart = LocalDate.now().minusDays(40).atStartOfDay();
        List<EtfCandle1d> rows = etfCandle1dRepository
                .findByEtfIdInAndCandleTimeGreaterThanEqualOrderByEtfIdAscCandleTimeDesc(etfIds, windowStart);
        Map<Long, List<EtfCandle1d>> byEtf = new HashMap<>();
        for (EtfCandle1d c : rows) {
            List<EtfCandle1d> list = byEtf.computeIfAbsent(c.getEtfId(), k -> new ArrayList<>());
            if (list.size() < 2) {
                list.add(c); // 이미 candleTime desc 정렬 → 앞 2개가 최신 2개
            }
        }
        return byEtf;
    }

    private RankedItem toRankedItem(Etf etf, List<EtfCandle1d> latestTwo,
                                    EtfRealtimeCacheService.PriceSnapshot cached) {
        if (latestTwo == null || latestTwo.isEmpty()) {
            return null;
        }

        EtfCandle1d today = latestTwo.get(0);
        LiveQuote quote = buildQuote(today, latestTwo, cached);
        long marketCap = quote.currentPrice() * etf.getListing();

        EtfListItem listItem = EtfListItem.of(
                etf, quote.currentPrice(), quote.priceChange(), quote.changeRate(), quote.volume(), quote.tradeAmount()
        );

        return new RankedItem(listItem, quote.volume(), quote.tradeAmount(), quote.changeRate(), marketCap, etf.getName());
    }

    // 단일 종목(상세) 경로 — 캐시를 직접 조회한다(N+1 무관).
    private LiveQuote resolveQuote(String code, EtfCandle1d today, List<EtfCandle1d> latestTwo) {
        return buildQuote(today, latestTwo, etfRealtimeCacheService.getCachedPrice(code).orElse(null));
    }

    // 시세 산출 공통 로직. cached가 있으면 실시간 캐시 기준(WS와 동일 출처), 없으면 당일 일봉 기준.
    private LiveQuote buildQuote(EtfCandle1d today, List<EtfCandle1d> latestTwo,
                                 EtfRealtimeCacheService.PriceSnapshot cached) {
        if (cached != null) {
            // 거래대금도 실시간 누적값을 우선 사용. 캐시 값이 0(미수신 등)이면 당일 일봉 거래대금으로 폴백.
            long tradeAmount = cached.tradeAmount() > 0 ? cached.tradeAmount() : today.getTradeAmount();
            return new LiveQuote(cached.price(), cached.change(), cached.changeRate(), cached.volume(), tradeAmount);
        }

        // 캐시 없으면(장 시작 전·피드 미수신) 당일 일봉 종가 기준으로 계산한다.
        long previousClose = latestTwo.size() > 1 ? latestTwo.get(1).getClosePrice() : today.getClosePrice();
        long priceChange = today.getClosePrice() - previousClose;
        double changeRate = previousClose == 0 ? 0.0 : (priceChange * 100.0) / previousClose;
        return new LiveQuote(today.getClosePrice(), priceChange, changeRate, today.getVolume(), today.getTradeAmount());
    }

    private Comparator<RankedItem> comparatorFor(String sort) {
        String key = sort == null ? "trade_amount" : sort;
        return switch (key) {
            case "volume" -> Comparator.comparingLong(RankedItem::volume).reversed();
            case "rate_asc" -> Comparator.comparingDouble(RankedItem::changeRate);
            case "rate_desc" -> Comparator.comparingDouble(RankedItem::changeRate).reversed();
            case "market_cap" -> Comparator.comparingLong(RankedItem::marketCap).reversed();
            case "name" -> Comparator.comparing(RankedItem::name);
            default -> Comparator.comparingLong(RankedItem::tradeAmount).reversed();
        };
    }

    private record RankedItem(EtfListItem listItem, long volume, long tradeAmount, double changeRate,
                               long marketCap, String name) {
    }

    private record LiveQuote(long currentPrice, long priceChange, double changeRate, long volume, long tradeAmount) {
    }
}
