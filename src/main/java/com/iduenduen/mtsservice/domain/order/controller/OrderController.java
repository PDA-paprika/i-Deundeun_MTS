package com.iduenduen.mtsservice.domain.order.controller;

import com.iduenduen.mtsservice.common.response.ApiResponse;
import com.iduenduen.mtsservice.common.status.SuccessStatus;
import com.iduenduen.mtsservice.domain.order.dto.OrderRequest;
import com.iduenduen.mtsservice.domain.order.dto.OrderResponse;
import com.iduenduen.mtsservice.domain.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Tag(name = "Order", description = "주문 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 제출", description = "매수/매도 주문을 제출합니다.")
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(
            @RequestBody OrderRequest request) {
        return ApiResponse.success(SuccessStatus.SUCCESS_200, orderService.submitOrder(request));
    }
}