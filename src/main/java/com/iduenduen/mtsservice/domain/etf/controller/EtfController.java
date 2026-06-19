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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "ETF", description = "ETF 조회 API")
public class EtfController {

    private static final int MAX_CHART_LIMIT = 500;

    private final EtfQueryService etfQueryService;
    private final EtfChartService etfChartService;
    private final EtfOrderBookService etfOrderBookService;

    @GetMapping("/list")
    @Operation(
            summary = "ETF 목록 조회",
            description = "검색어와 정렬 조건을 기준으로 ETF 목록을 페이징하여 조회합니다."
    )
    public ResponseEntity<ApiResponse<EtfListResponse>> getEtfList(
            @RequestParam(defaultValue = "trade_amount") String sort,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        EtfListResponse response = etfQueryService.getEtfList(sort, q, page, limit);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "ETF 검색",
            description = "ETF 이름 또는 종목 코드로 검색합니다."
    )
    public ResponseEntity<ApiResponse<EtfListResponse>> searchEtfs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        EtfListResponse response = etfQueryService.searchEtfs(keyword, page, limit);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @GetMapping("/{etfCode}")
    @Operation(
            summary = "ETF 상세 조회",
            description = "ETF 종목 코드에 해당하는 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<EtfDetailResponse>> getEtfDetail(@PathVariable String etfCode) {
        EtfDetailResponse response = etfQueryService.getEtfDetail(etfCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }

    @GetMapping("/{etfCode}/chart")
    @Operation(
            summary = "ETF 차트 조회",
            description = "간격(interval)과 기간(from, to) 조건으로 ETF 캔들 차트를 조회합니다."
    )
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
    @Operation(
            summary = "ETF 호가 조회",
            description = "LS API에서 최신 호가 스냅샷을 조회한 뒤 응답합니다."
    )
    public ResponseEntity<ApiResponse<EtfOrderBookResponse>> getEtfOrderBook(@PathVariable String etfCode) {
        etfOrderBookService.fetchAndSaveSnapshot(etfCode);
        EtfOrderBookResponse response = etfOrderBookService.getOrderBook(etfCode);
        return ApiResponse.success(SuccessStatus.SUCCESS_200, response);
    }
}
