package com.iduenduen.mtsservice.common.ls.stock.dto;

import lombok.Getter;

@Getter
public class LsUnifiedMinuteCandleRequest {

    private final T8452InBlock t8452InBlock;

    private LsUnifiedMinuteCandleRequest(String shcode, int ncnt, int qrycnt, String sdate, String edate,
                                          String ctsDate, String ctsTime, String exchgubun) {
        this.t8452InBlock = new T8452InBlock(shcode, ncnt, qrycnt, sdate, edate, ctsDate, ctsTime, exchgubun);
    }

    public static LsUnifiedMinuteCandleRequest of(String shcode, int ncnt, int qrycnt, String sdate, String edate, String exchgubun) {
        return new LsUnifiedMinuteCandleRequest(shcode, ncnt, qrycnt, sdate, edate, "", "", exchgubun);
    }

    public static LsUnifiedMinuteCandleRequest ofContinue(String shcode, int ncnt, int qrycnt, String sdate, String edate,
                                                           String ctsDate, String ctsTime, String exchgubun) {
        return new LsUnifiedMinuteCandleRequest(shcode, ncnt, qrycnt, sdate, edate, ctsDate, ctsTime, exchgubun);
    }

    @Getter
    public static class T8452InBlock {
        private final String shcode;
        private final int ncnt;
        private final int qrycnt;
        private final String nday;
        private final String sdate;
        private final String stime;
        private final String edate;
        private final String etime;
        private final String cts_date;
        private final String cts_time;
        private final String comp_yn;
        private final String exchgubun;

        private T8452InBlock(String shcode, int ncnt, int qrycnt, String sdate, String edate,
                              String ctsDate, String ctsTime, String exchgubun) {
            this.shcode = shcode;
            this.ncnt = ncnt;
            this.qrycnt = qrycnt;
            this.nday = "0";
            this.sdate = sdate;
            this.stime = "";
            this.edate = edate;
            this.etime = "";
            this.cts_date = ctsDate;
            this.cts_time = ctsTime;
            this.comp_yn = "N";
            this.exchgubun = exchgubun;
        }
    }
}
