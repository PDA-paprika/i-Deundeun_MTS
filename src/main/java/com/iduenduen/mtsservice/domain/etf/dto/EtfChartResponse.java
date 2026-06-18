package com.iduenduen.mtsservice.domain.etf.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class EtfChartResponse {

    private final String etfCode;
    private final String interval;
    private final List<ChartCandle> candles;

    private EtfChartResponse(String etfCode, String interval, List<ChartCandle> candles) {
        this.etfCode = etfCode;
        this.interval = interval;
        this.candles = candles;
    }

    public static EtfChartResponse of(String etfCode, String interval, List<ChartCandle> candles) {
        return new EtfChartResponse(etfCode, interval, candles);
    }
}
