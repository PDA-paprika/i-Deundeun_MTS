package com.iduenduen.mtsservice.domain.etf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_book_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderBookSnapshot {

    public static final String SIDE_ASK = "ask";
    public static final String SIDE_BID = "bid";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etf_id", nullable = false)
    private Long etfId;

    @Column(nullable = false, length = 3)
    private String side;

    @Column(nullable = false)
    private int step;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private long qty;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    private OrderBookSnapshot(Long etfId, String side, int step, long price, long qty, LocalDateTime snapshotAt) {
        this.etfId = etfId;
        this.side = side;
        this.step = step;
        this.price = price;
        this.qty = qty;
        this.snapshotAt = snapshotAt;
    }

    public static OrderBookSnapshot of(Long etfId, String side, int step, long price, long qty, LocalDateTime snapshotAt) {
        return new OrderBookSnapshot(etfId, side, step, price, qty, snapshotAt);
    }
}
