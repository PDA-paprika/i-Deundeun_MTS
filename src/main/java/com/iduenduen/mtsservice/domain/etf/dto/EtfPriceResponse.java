package com.iduenduen.mtsservice.domain.etf.dto;

import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EtfPriceResponse {

    private final long openPrice;
    private final long highPrice;
    private final long lowPrice;
    private final long closePrice;
    private final long volume;
    private final LocalDateTime candleTime;

    private EtfPriceResponse(EtfCandle1m candle) {
        this.openPrice = candle.getOpenPrice();
        this.highPrice = candle.getHighPrice();
        this.lowPrice = candle.getLowPrice();
        this.closePrice = candle.getClosePrice();
        this.volume = candle.getVolume();
        this.candleTime = candle.getCandleTime();
    }

    public static EtfPriceResponse from(EtfCandle1m candle) {
        return new EtfPriceResponse(candle);
    }
}
