package com.iduenduen.mtsservice.domain.order.entity;

import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_histories")
@Getter
@NoArgsConstructor
public class ExecutionHistory {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "order_id", columnDefinition = "CHAR(36)", nullable = false)
    private String orderId;

    @Column(name = "account_id", columnDefinition = "CHAR(36)", nullable = false)
    private String accountId;

    @Column(name = "parent_id", columnDefinition = "CHAR(36)", nullable = false)
    private String parentId;

    @Column(name = "etf_id", columnDefinition = "BINARY(16)", nullable = false)
    private byte[] etfId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", length = 4, nullable = false)
    private OrderSide side;

    @Column(name = "executed_qty", nullable = false)
    private int executedQty;

    @Column(name = "executed_price", nullable = false)
    private long executedPrice;

    @Column(name = "fee", nullable = false)
    private int fee;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
