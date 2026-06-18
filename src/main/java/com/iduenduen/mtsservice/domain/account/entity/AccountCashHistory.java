package com.iduenduen.mtsservice.domain.account.entity;

import com.iduenduen.mtsservice.domain.account.enums.CashEventType;
import com.iduenduen.mtsservice.domain.account.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_cash_histories")
@Getter
@NoArgsConstructor
public class AccountCashHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20, nullable = false)
    private CashEventType eventType;

    @Column(name = "amount_delta", nullable = false)
    private long amountDelta;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "reference_id")
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 20)
    private ReferenceType referenceType;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}