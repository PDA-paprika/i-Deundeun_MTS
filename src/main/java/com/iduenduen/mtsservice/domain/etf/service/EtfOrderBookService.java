package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.exception.GeneralException;
import com.iduenduen.mtsservice.common.status.ErrorStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfOrderBookResponse;
import com.iduenduen.mtsservice.domain.etf.dto.OrderBookLevel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EtfOrderBookService {

    private static final int LEVELS = 5;

    private final EtfRealtimeCacheService etfRealtimeCacheService;

    public EtfOrderBookResponse getOrderBook(String code) {
        EtfRealtimeCacheService.OrderBookSnapshotCache cache = etfRealtimeCacheService.getCachedOrderBook(code)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        List<OrderBookLevel> asks = new ArrayList<>();
        List<OrderBookLevel> bids = new ArrayList<>();
        for (int i = 0; i < LEVELS; i++) {
            int step = i + 1;
            asks.add(OrderBookLevel.of(step, cache.askPrices()[i], cache.askQtys()[i]));
            bids.add(OrderBookLevel.of(step, cache.bidPrices()[i], cache.bidQtys()[i]));
        }
        return EtfOrderBookResponse.of(code, cache.updatedAt(), asks, bids);
    }
}
