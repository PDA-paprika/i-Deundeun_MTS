package com.iduenduen.mtsservice.domain.etf.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "etf_candles_1w")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtfCandle1w {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etf_id", nullable = false)
    private Long etfId;

    @Column(name = "open_price", nullable = false)
    private long openPrice;

    @Column(name = "high_price", nullable = false)
    private long highPrice;

    @Column(name = "low_price", nullable = false)
    private long lowPrice;

    @Column(name = "close_price", nullable = false)
    private long closePrice;

    @Column(nullable = false)
    private long volume;

    @Column(name = "trade_amount", nullable = false)
    private long tradeAmount;

    @Column(name = "candle_time", nullable = false)
    private LocalDateTime candleTime;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private EtfCandle1w(Long etfId, long openPrice, long highPrice, long lowPrice, long closePrice,
                         long volume, long tradeAmount, LocalDateTime candleTime) {
        this.etfId = etfId;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradeAmount = tradeAmount;
        this.candleTime = candleTime;
    }

    public static EtfCandle1w of(Long etfId, long openPrice, long highPrice, long lowPrice, long closePrice,
                                  long volume, long tradeAmount, LocalDateTime candleTime) {
        return new EtfCandle1w(etfId, openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount, candleTime);
    }

    public void updateSnapshot(long openPrice, long highPrice, long lowPrice, long closePrice,
                                long volume, long tradeAmount) {
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradeAmount = tradeAmount;
    }
}
