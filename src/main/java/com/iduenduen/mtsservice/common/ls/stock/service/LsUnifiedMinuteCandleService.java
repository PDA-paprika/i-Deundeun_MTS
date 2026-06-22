package com.iduenduen.mtsservice.common.ls.stock.service;

import com.iduenduen.mtsservice.common.ls.client.LsRestClient;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedMinuteCandleRequest;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedMinuteCandleResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LsUnifiedMinuteCandleService {

    private static final String PATH = "/stock/chart";
    private static final String TR_CD = "t8452";
    private static final int QRYCNT = 500;

    private final LsRestClient lsRestClient;

    public LsUnifiedMinuteCandleResponse getUnifiedMinuteCandles(String shcode, int ncnt, String sdate, String edate, String exchgubun) {
        LsUnifiedMinuteCandleRequest request = LsUnifiedMinuteCandleRequest.of(shcode, ncnt, QRYCNT, sdate, edate, exchgubun);
        return lsRestClient.post(PATH, TR_CD, request, LsUnifiedMinuteCandleResponse.class);
    }

    public LsUnifiedMinuteCandleResponse getUnifiedMinuteCandlesContinue(String shcode, int ncnt, String sdate, String edate,
                                                                          String ctsDate, String ctsTime, String exchgubun) {
        LsUnifiedMinuteCandleRequest request = LsUnifiedMinuteCandleRequest.ofContinue(shcode, ncnt, QRYCNT, sdate, edate, ctsDate, ctsTime, exchgubun);
        return lsRestClient.post(PATH, TR_CD, request, LsUnifiedMinuteCandleResponse.class, "Y", ctsDate + ctsTime);
    }
}
