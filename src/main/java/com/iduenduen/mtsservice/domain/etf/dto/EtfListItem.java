package com.iduenduen.mtsservice.domain.etf.dto;

import com.iduenduen.mtsservice.domain.etf.entity.Etf;

import lombok.Getter;

import java.util.UUID;

@Getter
public class EtfListItem {

    private final UUID etfId;
    private final String etfCode;
    private final String etfName;
    private final String logoImg;
    private final long currentPrice;
    private final long priceChange;
    private final double changeRate;
    private final long volume;
    private final long tradeAmount;

    private EtfListItem(Etf etf, long currentPrice, long priceChange, double changeRate,
                         long volume, long tradeAmount) {
        this.etfId = etf.getId();
        this.etfCode = etf.getCode();
        this.etfName = etf.getName();
        this.logoImg = etf.getLogoImg();
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.changeRate = changeRate;
        this.volume = volume;
        this.tradeAmount = tradeAmount;
    }

    public static EtfListItem of(Etf etf, long currentPrice, long priceChange, double changeRate,
                                  long volume, long tradeAmount) {
        return new EtfListItem(etf, currentPrice, priceChange, changeRate, volume, tradeAmount);
    }
}
