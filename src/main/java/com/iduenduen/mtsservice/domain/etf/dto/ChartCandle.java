package com.iduenduen.mtsservice.domain.etf.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChartCandle {

    private final LocalDateTime candleTime;
    private final long open;
    private final long high;
    private final long low;
    private final long close;
    private final long volume;

    private ChartCandle(LocalDateTime candleTime, long open, long high, long low, long close, long volume) {
        this.candleTime = candleTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public static ChartCandle of(LocalDateTime candleTime, long open, long high, long low, long close, long volume) {
        return new ChartCandle(candleTime, open, high, low, close, volume);
    }
}
