package com.iduenduen.mtsservice.domain.order.dto;

import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderRequest {
    private String accountId;
    private String etfCode;
    private OrderSide side;
    private OrderType orderType;
    private Long price;  // MARKET이면 null
    private int qty;
}
