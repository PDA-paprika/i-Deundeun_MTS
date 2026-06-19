package com.iduenduen.mtsservice.domain.etf.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfPriceResponse;
import com.iduenduen.mtsservice.domain.etf.entity.EtfCandle1m;
import com.iduenduen.mtsservice.domain.etf.service.EtfPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/etf")
@RequiredArgsConstructor
@Tag(name = "ETF", description = "ETF 시세 API")
public class EtfPriceController {

    private final EtfPriceService etfPriceService;

    @GetMapping("/{code}/price")
    @Operation(
            summary = "ETF 현재가 조회",
            description = "LS API에서 ETF 현재가를 조회하고 1분봉으로 저장한 뒤 응답합니다."
    )
    public ResponseEntity<ApiResponse<EtfPriceResponse>> getCurrentPrice(@PathVariable String code) {
        EtfCandle1m candle = etfPriceService.fetchAndSaveCurrentPrice(code);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, EtfPriceResponse.from(candle));
    }
}
