package com.iduenduen.mtsservice.domain.order.entity;

import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import com.iduenduen.mtsservice.domain.order.enums.OrderStatus;
import com.iduenduen.mtsservice.domain.order.enums.OrderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade_orders")
@Getter
@NoArgsConstructor
public class TradeOrder {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "account_id", columnDefinition = "CHAR(36)", nullable = false)
    private String accountId;

    @Column(name = "parent_id", columnDefinition = "CHAR(36)", nullable = false)
    private String parentId;

    @Column(name = "etf_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] etfId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", length = 4, nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 11, nullable = false)
    private OrderType orderType;

    @Column(name = "price")
    private Long price;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "remaining_qty", nullable = false)
    private int remainingQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
