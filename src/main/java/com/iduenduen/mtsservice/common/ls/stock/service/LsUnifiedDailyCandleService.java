package com.iduenduen.mtsservice.common.ls.stock.service;

import com.iduenduen.mtsservice.common.ls.client.LsRestClient;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedDailyCandleRequest;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedDailyCandleResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LsUnifiedDailyCandleService {

    private static final String PATH = "/stock/chart";
    private static final String TR_CD = "t8451";

    public static final String GUBUN_DAY = "2";
    public static final String GUBUN_WEEK = "3";
    public static final String GUBUN_MONTH = "4";

    private final LsRestClient lsRestClient;

    public LsUnifiedDailyCandleResponse getUnifiedDailyCandles(String shcode, String gubun, String sdate, String edate, String exchgubun) {
        LsUnifiedDailyCandleRequest request = LsUnifiedDailyCandleRequest.of(shcode, gubun, sdate, edate, exchgubun);
        return lsRestClient.post(PATH, TR_CD, request, LsUnifiedDailyCandleResponse.class);
    }

    public LsUnifiedDailyCandleResponse getUnifiedDailyCandlesContinue(String shcode, String gubun, String sdate, String edate,
                                                                         String ctsDate, String exchgubun) {
        LsUnifiedDailyCandleRequest request = LsUnifiedDailyCandleRequest.ofContinue(shcode, gubun, sdate, edate, ctsDate, exchgubun);
        return lsRestClient.post(PATH, TR_CD, request, LsUnifiedDailyCandleResponse.class, "Y", ctsDate);
    }
}
