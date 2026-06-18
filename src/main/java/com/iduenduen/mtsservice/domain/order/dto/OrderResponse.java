package com.iduenduen.mtsservice.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {
    private String orderId;
    private String status;
    private String message;
}
