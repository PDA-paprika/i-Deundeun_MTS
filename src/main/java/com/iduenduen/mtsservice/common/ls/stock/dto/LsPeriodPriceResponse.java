package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LsPeriodPriceResponse {

    private T1305OutBlock t1305OutBlock;
    private List<T1305OutBlock1> t1305OutBlock1;

    @Getter
    @Setter
    public static class T1305OutBlock {
        private String date;
    }

    @Getter
    @Setter
    public static class T1305OutBlock1 {
        private String date;
        private String open;
        private String high;
        private String low;
        private String close;
        private String volume;
        private String value;
        private String marketcap;
    }
}
