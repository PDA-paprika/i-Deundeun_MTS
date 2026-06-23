package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfDetailResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfListItem;
import com.iduenduen.mtsservice.domain.etf.dto.EtfListResponse;
import com.iduenduen.mtsservice.domain.etf.entity.Etf;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1d;
import com.iduenduen.mtsservice.domain.etf.repository.EtfCandle1dRepository;
import com.iduenduen.mtsservice.domain.etf.repository.EtfRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                today.getHighPrice(), today.getLowPrice(), quote.volume(), today.getTradeAmount()
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
                etf, quote.currentPrice(), quote.priceChange(), quote.changeRate(), quote.volume(), today.getTradeAmount()
        );

        return new RankedItem(listItem, quote.volume(), today.getTradeAmount(), quote.changeRate(), marketCap, etf.getName());
    }

    private LiveQuote resolveQuote(String code, EtfCandle1d today, List<EtfCandle1d> latestTwo) {
        long previousClose = latestTwo.size() > 1 ? latestTwo.get(1).getClosePrice() : today.getClosePrice();

        Optional<EtfRealtimeCacheService.PriceSnapshot> cached = etfRealtimeCacheService.getCachedPrice(code);
        long currentPrice = cached.map(EtfRealtimeCacheService.PriceSnapshot::price).orElse(today.getClosePrice());
        long volume = cached.map(EtfRealtimeCacheService.PriceSnapshot::volume).orElse(today.getVolume());

        long priceChange = currentPrice - previousClose;
        double changeRate = previousClose == 0 ? 0.0 : (priceChange * 100.0) / previousClose;
        return new LiveQuote(currentPrice, priceChange, changeRate, volume);
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

    private record LiveQuote(long currentPrice, long priceChange, double changeRate, long volume) {
    }
}
