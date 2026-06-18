package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;

@Getter
public class LsOrderBookRequest {

    private final T1101InBlock t1101InBlock;

    private LsOrderBookRequest(String shcode) {
        this.t1101InBlock = new T1101InBlock(shcode);
    }

    public static LsOrderBookRequest of(String shcode) {
        return new LsOrderBookRequest(shcode);
    }

    @Getter
    public static class T1101InBlock {
        private final String shcode;

        private T1101InBlock(String shcode) {
            this.shcode = shcode;
        }
    }
}
