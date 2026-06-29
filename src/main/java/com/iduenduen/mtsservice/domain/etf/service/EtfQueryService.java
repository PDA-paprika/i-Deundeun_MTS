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
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfQueryService {

    private final EtfRepository etfRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;
    private final EtfRealtimeCacheService etfRealtimeCacheService;

    public EtfDetailResponse getEtfDetailById(Long etfId) {
        Etf etf = etfRepository.findById(etfId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ETF_NOT_FOUND));

        List<EtfCandle1d> latestTwo = etfCandle1dRepository.findTop2ByEtfIdOrderByCandleTimeDesc(etf.getId());
        if (latestTwo.isEmpty()) {
            throw new GeneralException(ErrorStatus.ETF_NOT_FOUND);
        }

        EtfCandle1d today = latestTwo.get(0);
        DayOverDayChange change = computeChange(today, latestTwo);

        return EtfDetailResponse.of(
                etf, today.getClosePrice(), change.priceChange(), change.changeRate(),
                today.getHighPrice(), today.getLowPrice(), today.getVolume(), today.getTradeAmount()
        );
    }

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

        List<RankedItem> rankedItems = etfs.stream()
                .map(this::toRankedItem)
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

    private RankedItem toRankedItem(Etf etf) {
        List<EtfCandle1d> latestTwo = etfCandle1dRepository.findTop2ByEtfIdOrderByCandleTimeDesc(etf.getId());
        if (latestTwo.isEmpty()) {
            return null;
        }

        EtfCandle1d today = latestTwo.get(0);
        LiveQuote quote = resolveQuote(etf.getCode(), today, latestTwo);
        long marketCap = quote.currentPrice() * etf.getListing();

        EtfListItem listItem = EtfListItem.of(
                etf, quote.currentPrice(), quote.priceChange(), quote.changeRate(), quote.volume(), quote.tradeAmount()
        );

        return new RankedItem(listItem, quote.volume(), quote.tradeAmount(), quote.changeRate(), marketCap, etf.getName());
    }

    private LiveQuote resolveQuote(String code, EtfCandle1d today, List<EtfCandle1d> latestTwo) {
        // 실시간 캐시가 있으면 가격·전일대비·등락률·거래량을 그대로 사용한다(WS와 동일 출처).
        // → REST 초기값과 WS 실시간값의 기준이 같아 화면에서 값이 튀지 않는다.
        Optional<EtfRealtimeCacheService.PriceSnapshot> cached = etfRealtimeCacheService.getCachedPrice(code);
        if (cached.isPresent()) {
            EtfRealtimeCacheService.PriceSnapshot s = cached.get();
            // 거래대금도 실시간 누적값을 우선 사용. 캐시 값이 0(미수신 등)이면 당일 일봉 거래대금으로 폴백.
            long tradeAmount = s.tradeAmount() > 0 ? s.tradeAmount() : today.getTradeAmount();
            return new LiveQuote(s.price(), s.change(), s.changeRate(), s.volume(), tradeAmount);
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
