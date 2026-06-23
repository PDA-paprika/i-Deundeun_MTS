package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LsUnifiedDailyCandleResponse {

    private T8451OutBlock t8451OutBlock;
    private List<T8451OutBlock1> t8451OutBlock1;

    public boolean hasNext() {
        return t8451OutBlock != null
                && t8451OutBlock.getCts_date() != null
                && !t8451OutBlock.getCts_date().isBlank();
    }

    @Getter
    @Setter
    public static class T8451OutBlock {
        private String cts_date;
    }

    @Getter
    @Setter
    public static class T8451OutBlock1 {
        private String date;
        private String open;
        private String high;
        private String low;
        private String close;
        private String jdiff_vol;
        private String value;
    }
}
