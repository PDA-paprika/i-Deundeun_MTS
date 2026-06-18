package com.iduenduen.mtsservice.common.ls.stock.service;

import com.iduenduen.mtsservice.common.ls.client.LsRestClient;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsStockPriceRequest;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsStockPriceResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LsStockPriceService {

    private static final String PATH = "/stock/market-data";
    private static final String TR_CD = "t1102";
    private static final String EXCHANGE_KRX = "K";

    private final LsRestClient lsRestClient;

    public LsStockPriceResponse.T1102OutBlock getCurrentPrice(String shcode) {
        LsStockPriceRequest request = LsStockPriceRequest.of(shcode, EXCHANGE_KRX);
        LsStockPriceResponse response = lsRestClient.post(PATH, TR_CD, request, LsStockPriceResponse.class);
        return response.getT1102OutBlock();
    }
}
