package com.iduenduen.mtsservice.domain.etf.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfPriceResponse;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;
import com.iduenduen.mtsservice.domain.etf.service.EtfPriceService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/etf")
@RequiredArgsConstructor
public class EtfPriceController {

    private final EtfPriceService etfPriceService;

    @GetMapping("/{code}/price")
    public ResponseEntity<ApiResponse<EtfPriceResponse>> getCurrentPrice(@PathVariable String code) {
        EtfCandle1m candle = etfPriceService.fetchAndSaveCurrentPrice(code);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, EtfPriceResponse.from(candle));
    }
}
