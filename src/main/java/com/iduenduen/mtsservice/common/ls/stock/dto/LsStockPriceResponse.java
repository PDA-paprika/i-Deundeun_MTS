package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LsStockPriceResponse {

    private T1901OutBlock t1901OutBlock;

    @Getter
    @Setter
    public static class T1901OutBlock {
        private String shcode;
        private String hname;
        private String price;
        private String sign;
        private String change;
        private String diff;
        private String volume;
        private String open;
        private String high;
        private String low;
        private String value;
    }
}
