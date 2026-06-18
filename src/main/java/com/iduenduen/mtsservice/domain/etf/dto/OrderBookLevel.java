package com.iduenduen.mtsservice.domain.etf.dto;

import lombok.Getter;

@Getter
public class OrderBookLevel {

    private final int step;
    private final long price;
    private final long qty;

    private OrderBookLevel(int step, long price, long qty) {
        this.step = step;
        this.price = price;
        this.qty = qty;
    }

    public static OrderBookLevel of(int step, long price, long qty) {
        return new OrderBookLevel(step, price, qty);
    }
}
