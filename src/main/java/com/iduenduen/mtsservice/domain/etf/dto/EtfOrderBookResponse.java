package com.iduenduen.mtsservice.domain.etf.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class EtfOrderBookResponse {

    private final String etfCode;
    private final LocalDateTime snapshotAt;
    private final List<OrderBookLevel> asks;
    private final List<OrderBookLevel> bids;

    private EtfOrderBookResponse(String etfCode, LocalDateTime snapshotAt, List<OrderBookLevel> asks, List<OrderBookLevel> bids) {
        this.etfCode = etfCode;
        this.snapshotAt = snapshotAt;
        this.asks = asks;
        this.bids = bids;
    }

    public static EtfOrderBookResponse of(String etfCode, LocalDateTime snapshotAt, List<OrderBookLevel> asks, List<OrderBookLevel> bids) {
        return new EtfOrderBookResponse(etfCode, snapshotAt, asks, bids);
    }
}
