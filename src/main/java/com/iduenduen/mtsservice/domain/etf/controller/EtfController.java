package com.iduenduen.mtsservice.domain.etf.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfChartResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfDetailResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfListResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfOrderBookResponse;
import com.iduenduen.mtsservice.domain.etf.service.EtfChartService;
import com.iduenduen.mtsservice.domain.etf.service.EtfOrderBookService;
import com.iduenduen.mtsservice.domain.etf.service.EtfQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/etf")
@RequiredArgsConstructor
public class EtfController {

    private static final int MAX_CHART_LIMIT = 500;

    private final EtfQueryService etfQueryService;
    private final EtfChartService etfChartService;
    private final EtfOrderBookService etfOrderBookService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<EtfListResponse>> getEtfList(
            @RequestParam(defaultValue = "trade_amount") String sort,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        EtfListResponse response = etfQueryService.getEtfList(sort, q, page, limit);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @GetMapping("/{etfCode}")
    public ResponseEntity<ApiResponse<EtfDetailResponse>> getEtfDetail(@PathVariable String etfCode) {
        EtfDetailResponse response = etfQueryService.getEtfDetail(etfCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @GetMapping("/{etfCode}/chart")
    public ResponseEntity<ApiResponse<EtfChartResponse>> getEtfChart(
            @PathVariable String etfCode,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        int clampedLimit = Math.min(limit, MAX_CHART_LIMIT);
        EtfChartResponse response = etfChartService.getChart(etfCode, interval, from, to, clampedLimit);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @GetMapping("/{etfCode}/orderbook")
    public ResponseEntity<ApiResponse<EtfOrderBookResponse>> getEtfOrderBook(@PathVariable String etfCode) {
        etfOrderBookService.fetchAndSaveSnapshot(etfCode);
        EtfOrderBookResponse response = etfOrderBookService.getOrderBook(etfCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }
}
