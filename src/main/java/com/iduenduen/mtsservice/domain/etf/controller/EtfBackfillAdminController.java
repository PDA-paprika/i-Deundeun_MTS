package com.iduenduen.mtsservice.domain.etf.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.etf.service.EtfBackfillAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/etf")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "ETF 데이터 수동 적재 API")
public class EtfBackfillAdminController {

    private final EtfBackfillAdminService etfBackfillAdminService;

    @PostMapping("/seed")
    @Operation(summary = "ETF 종목 시딩", description = "아직 등록 안 된 종목만 LS API로 조회해서 시딩합니다.")
    public ResponseEntity<ApiResponse<Void>> seed() {
        etfBackfillAdminService.seedAll();
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @PostMapping("/{code}/backfill")
    @Operation(
            summary = "특정 종목 캔들 강제 백필",
            description = "일/주/월봉 + 최근 30일 분봉(1/10/30/60분)을 강제로 다시 채웁니다. 이미 있는 데이터는 upsert로 갱신됩니다."
    )
    public ResponseEntity<ApiResponse<Void>> backfillOne(@PathVariable String code) {
        etfBackfillAdminService.backfillOne(code);
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @PostMapping("/backfill-all")
    @Operation(
            summary = "전종목 캔들 강제 백필 (백그라운드)",
            description = "76개 전종목을 백그라운드에서 강제로 다시 백필합니다. 즉시 응답하고 서버 로그로 진행상황을 확인합니다."
    )
    public ResponseEntity<ApiResponse<Void>> backfillAll() {
        etfBackfillAdminService.backfillAllAsync();
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }
}
