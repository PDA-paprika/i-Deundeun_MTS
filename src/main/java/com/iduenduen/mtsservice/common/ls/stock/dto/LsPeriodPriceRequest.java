package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;

@Getter
public class LsPeriodPriceRequest {

    private final T1305InBlock t1305InBlock;

    private LsPeriodPriceRequest(String shcode, int dwmcode, String date, int cnt, String exchgubun) {
        this.t1305InBlock = new T1305InBlock(shcode, dwmcode, date, 0, cnt, exchgubun);
    }

    public static LsPeriodPriceRequest of(String shcode, int dwmcode, String date, int cnt, String exchgubun) {
        return new LsPeriodPriceRequest(shcode, dwmcode, date, cnt, exchgubun);
    }

    @Getter
    public static class T1305InBlock {
        private final String shcode;
        private final int dwmcode;
        private final String date;
        private final int idx;
        private final int cnt;
        private final String exchgubun;

        private T1305InBlock(String shcode, int dwmcode, String date, int idx, int cnt, String exchgubun) {
            this.shcode = shcode;
            this.dwmcode = dwmcode;
            this.date = date;
            this.idx = idx;
            this.cnt = cnt;
            this.exchgubun = exchgubun;
        }
    }
}
