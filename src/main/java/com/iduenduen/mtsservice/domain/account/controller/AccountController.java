package com.iduenduen.mtsservice.domain.account.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.account.dto.AccountBalanceResponse;
import com.iduenduen.mtsservice.domain.account.dto.AccountSummaryResponse;
import com.iduenduen.mtsservice.domain.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
@Tag(name = "Account", description = "계좌 API")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/balance")
    @Operation(summary = "예수금 조회", description = "주문 가능 예수금을 조회합니다.")
    public ResponseEntity<ApiResponse<AccountBalanceResponse>> getBalance(
            @RequestParam Long accountId) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, accountService.getBalance(accountId));
    }

    @GetMapping("/summary")
    @Operation(summary = "계좌 요약 조회", description = "예수금 및 보유 종목 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<AccountSummaryResponse>> getSummary(
            @RequestParam Long accountId) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, accountService.getSummary(accountId));
    }
}