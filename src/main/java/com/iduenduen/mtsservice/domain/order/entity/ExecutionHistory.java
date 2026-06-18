package com.iduenduen.mtsservice.domain.order.entity;

import com.iduenduen.mtsservice.domain.order.enums.OrderSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_histories")
@Getter
@NoArgsConstructor
public class ExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "etf_id", nullable = false)
    private Long etfId;

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