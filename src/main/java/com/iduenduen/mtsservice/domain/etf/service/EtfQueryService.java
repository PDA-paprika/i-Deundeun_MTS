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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EtfQueryService {

    private final EtfRepository etfRepository;
    private final EtfCandle1dRepository etfCandle1dRepository;

    public EtfDetailResponse getEtfDetail(String code) {
        Etf etf = etfRepository.findByCode(code)
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

    public EtfListResponse getEtfList(String sort, String q, int page, int limit) {
        List<Etf> etfs = (q == null || q.isBlank())
                ? etfRepository.findAll()
                : etfRepository.findByNameContainingIgnoreCase(q);

        List<RankedItem> rankedItems = etfs.stream()
                .map(this::toRankedItem)
                .filter(item -> item != null)
                .sorted(comparatorFor(sort))
                .toList();

        long totalCount = rankedItems.size();
        int fromIndex = Math.min(page * limit, rankedItems.size());
        int toIndex = Math.min(fromIndex + limit, rankedItems.size());

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
        DayOverDayChange change = computeChange(today, latestTwo);
        long marketCap = today.getClosePrice() * etf.getListing();

        EtfListItem listItem = EtfListItem.of(
                etf, today.getClosePrice(), change.priceChange(), change.changeRate(), today.getVolume(), today.getTradeAmount()
        );

        return new RankedItem(listItem, today.getVolume(), today.getTradeAmount(), change.changeRate(), marketCap, etf.getName());
    }

    private DayOverDayChange computeChange(EtfCandle1d today, List<EtfCandle1d> latestTwo) {
        long previousClose = latestTwo.size() > 1 ? latestTwo.get(1).getClosePrice() : today.getClosePrice();
        long priceChange = today.getClosePrice() - previousClose;
        double changeRate = previousClose == 0 ? 0.0 : (priceChange * 100.0) / previousClose;
        return new DayOverDayChange(priceChange, changeRate);
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

    private record DayOverDayChange(long priceChange, double changeRate) {
    }
}
