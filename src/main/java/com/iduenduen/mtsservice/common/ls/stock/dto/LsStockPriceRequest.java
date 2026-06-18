package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;

@Getter
public class LsStockPriceRequest {

    private final T1102InBlock t1102InBlock;

    private LsStockPriceRequest(String shcode, String exchgubun) {
        this.t1102InBlock = new T1102InBlock(shcode, exchgubun);
    }

    public static LsStockPriceRequest of(String shcode, String exchgubun) {
        return new LsStockPriceRequest(shcode, exchgubun);
    }

    @Getter
    public static class T1102InBlock {
        private final String shcode;
        private final String exchgubun;

        private T1102InBlock(String shcode, String exchgubun) {
            this.shcode = shcode;
            this.exchgubun = exchgubun;
        }
    }
}
