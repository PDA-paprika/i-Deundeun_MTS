package com.iduenduen.mtsservice.common.ls.stock.service;

import com.iduenduen.mtsservice.common.ls.client.LsRestClient;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsOrderBookRequest;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsOrderBookResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LsOrderBookService {

    private static final String PATH = "/stock/market-data";
    private static final String TR_CD = "t1101";

    private final LsRestClient lsRestClient;

    public LsOrderBookResponse.T1101OutBlock getOrderBook(String shcode) {
        LsOrderBookRequest request = LsOrderBookRequest.of(shcode);
        LsOrderBookResponse response = lsRestClient.post(PATH, TR_CD, request, LsOrderBookResponse.class);
        return response.getT1101OutBlock();
    }
}
