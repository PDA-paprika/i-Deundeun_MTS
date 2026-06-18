package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LsOrderBookResponse {

    private T1101OutBlock t1101OutBlock;

    @Getter
    @Setter
    public static class T1101OutBlock {
        private String shcode;
        private String offerho1;
        private String bidho1;
        private String offerrem1;
        private String bidrem1;
        private String offerho2;
        private String bidho2;
        private String offerrem2;
        private String bidrem2;
        private String offerho3;
        private String bidho3;
        private String offerrem3;
        private String bidrem3;
        private String offerho4;
        private String bidho4;
        private String offerrem4;
        private String bidrem4;
        private String offerho5;
        private String bidho5;
        private String offerrem5;
        private String bidrem5;
    }
}
