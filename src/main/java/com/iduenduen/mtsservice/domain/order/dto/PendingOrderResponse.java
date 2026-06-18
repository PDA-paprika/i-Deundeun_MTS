package com.iduenduen.mtsservice.domain.order.dto;

import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PendingOrderResponse {
    private Long orderId;
    private String etfCode;
    private OrderSide side;
    private OrderType orderType;
    private Long price;
    private int qty;
    private int remainingQty;
    private OrderStatus status;
    private LocalDateTime orderedAt;
}