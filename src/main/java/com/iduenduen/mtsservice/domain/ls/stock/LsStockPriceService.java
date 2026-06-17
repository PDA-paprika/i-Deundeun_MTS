package com.iduenduen.mtsservice.domain.ls.stock;

import com.iduenduen.mtsservice.domain.ls.client.LsRestClient;
import com.iduenduen.mtsservice.domain.ls.stock.dto.LsStockPriceRequest;
import com.iduenduen.mtsservice.domain.ls.stock.dto.LsStockPriceResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LsStockPriceService {

    private static final String PATH = "/stock/etf";
    private static final String TR_CD = "t1901";

    private final LsRestClient lsRestClient;

    public LsStockPriceResponse.T1901OutBlock getCurrentPrice(String shcode) {
        LsStockPriceRequest request = LsStockPriceRequest.of(shcode);
        LsStockPriceResponse response = lsRestClient.post(PATH, TR_CD, request, LsStockPriceResponse.class);
        return response.getT1901OutBlock();
    }
}
