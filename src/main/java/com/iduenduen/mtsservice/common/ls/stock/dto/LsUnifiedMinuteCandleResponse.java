package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LsUnifiedMinuteCandleResponse {

    private T8452OutBlock t8452OutBlock;
    private List<T8452OutBlock1> t8452OutBlock1;

    public boolean hasNext() {
        return t8452OutBlock != null
                && t8452OutBlock.getCts_date() != null
                && !t8452OutBlock.getCts_date().isBlank();
    }

    @Getter
    @Setter
    public static class T8452OutBlock {
        private String cts_date;
        private String cts_time;
    }

    @Getter
    @Setter
    public static class T8452OutBlock1 {
        private String date;
        private String time;
        private String open;
        private String high;
        private String low;
        private String close;
        private String jdiff_vol;
        private String value;
    }
}
