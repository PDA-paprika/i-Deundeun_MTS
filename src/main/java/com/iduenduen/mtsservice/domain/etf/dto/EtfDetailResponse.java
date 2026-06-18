package com.iduenduen.mtsservice.domain.etf.dto;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;

import lombok.Getter;

@Getter
public class EtfDetailResponse {

    private final Long etfId;
    private final String etfCode;
    private final String etfName;
    private final String logoImg;
    private final long currentPrice;
    private final long priceChange;
    private final double changeRate;
    private final long highPrice;
    private final long lowPrice;
    private final long volume;
    private final long tradeAmount;

    private EtfDetailResponse(Etf etf, long currentPrice, long priceChange, double changeRate,
                               long highPrice, long lowPrice, long volume, long tradeAmount) {
        this.etfId = etf.getId();
        this.etfCode = etf.getCode();
        this.etfName = etf.getName();
        this.logoImg = etf.getLogoImg();
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.changeRate = changeRate;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.tradeAmount = tradeAmount;
    }

    public static EtfDetailResponse of(Etf etf, long currentPrice, long priceChange, double changeRate,
                                        long highPrice, long lowPrice, long volume, long tradeAmount) {
        return new EtfDetailResponse(etf, currentPrice, priceChange, changeRate, highPrice, lowPrice, volume, tradeAmount);
    }
}
