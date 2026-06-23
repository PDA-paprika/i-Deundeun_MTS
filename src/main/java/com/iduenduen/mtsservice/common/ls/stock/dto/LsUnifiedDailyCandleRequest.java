package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;

@Getter
public class LsUnifiedDailyCandleRequest {

    private final T8451InBlock t8451InBlock;

    private LsUnifiedDailyCandleRequest(String shcode, String gubun, String sdate, String edate, String ctsDate, String exchgubun) {
        this.t8451InBlock = new T8451InBlock(shcode, gubun, sdate, edate, ctsDate, exchgubun);
    }

    public static LsUnifiedDailyCandleRequest of(String shcode, String gubun, String sdate, String edate, String exchgubun) {
        return new LsUnifiedDailyCandleRequest(shcode, gubun, sdate, edate, "", exchgubun);
    }

    public static LsUnifiedDailyCandleRequest ofContinue(String shcode, String gubun, String sdate, String edate,
                                                          String ctsDate, String exchgubun) {
        return new LsUnifiedDailyCandleRequest(shcode, gubun, sdate, edate, ctsDate, exchgubun);
    }

    @Getter
    public static class T8451InBlock {
        private final String shcode;
        private final String gubun;
        private final int qrycnt;
        private final String sdate;
        private final String edate;
        private final String cts_date;
        private final String comp_yn;
        private final String sujung;
        private final String exchgubun;

        private T8451InBlock(String shcode, String gubun, String sdate, String edate, String ctsDate, String exchgubun) {
            this.shcode = shcode;
            this.gubun = gubun;
            this.qrycnt = 500;
            this.sdate = sdate;
            this.edate = edate;
            this.cts_date = ctsDate;
            this.comp_yn = "N";
            this.sujung = "Y";
            this.exchgubun = exchgubun;
        }
    }
}
