package com.iduenduen.mtsservice.domain.etf.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.etf.dto.EtfDetailResponse;
import com.iduenduen.mtsservice.domain.etf.dto.EtfListResponse;
import com.iduenduen.mtsservice.domain.etf.service.EtfQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/etf")
@RequiredArgsConstructor
public class EtfController {

    private final EtfQueryService etfQueryService;

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
}
