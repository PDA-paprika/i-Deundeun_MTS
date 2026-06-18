package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;

@Getter
public class LsStockPriceRequest {

    private final T1901InBlock t1901InBlock;

    private LsStockPriceRequest(String shcode) {
        this.t1901InBlock = new T1901InBlock(shcode);
    }

    public static LsStockPriceRequest of(String shcode) {
        return new LsStockPriceRequest(shcode);
    }

    @Getter
    public static class T1901InBlock {
        private final String shcode;

        private T1901InBlock(String shcode) {
            this.shcode = shcode;
        }
    }
}
