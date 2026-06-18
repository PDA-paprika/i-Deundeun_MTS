package com.iduenduen.mtsservice.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {
    private Long orderId;
    private String status;
    private String message;
}
