package com.iduenduen.mtsservice.common.ls.stock.service;

import com.iduenduen.mtsservice.common.ls.client.LsRestClient;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsPeriodPriceRequest;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsPeriodPriceResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LsPeriodPriceService {

    private static final String PATH = "/stock/market-data";
    private static final String TR_CD = "t1305";
    private static final String EXCHANGE_KRX = "K";

    public static final int DWM_DAY = 1;
    public static final int DWM_WEEK = 2;
    public static final int DWM_MONTH = 3;

    private final LsRestClient lsRestClient;

    public List<LsPeriodPriceResponse.T1305OutBlock1> getPeriodPrices(String shcode, int dwmcode, int cnt) {
        LsPeriodPriceRequest request = LsPeriodPriceRequest.of(shcode, dwmcode, "", cnt, EXCHANGE_KRX);
        LsPeriodPriceResponse response = lsRestClient.post(PATH, TR_CD, request, LsPeriodPriceResponse.class);
        return response.getT1305OutBlock1();
    }
}
