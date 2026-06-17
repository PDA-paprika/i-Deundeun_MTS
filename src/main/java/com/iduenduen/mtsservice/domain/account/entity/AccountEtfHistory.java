package com.iduenduen.mtsservice.domain.account.entity;

import com.iduenduen.mtsservice.domain.account.enums.EtfEventType;
import com.iduenduen.mtsservice.domain.account.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_etf_histories")
@Getter
@NoArgsConstructor
public class AccountEtfHistory {

    @Id
    @UuidGenerator
    @Column(name = "id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "account_id", columnDefinition = "CHAR(36)", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20, nullable = false)
    private EtfEventType eventType;

    @Column(name = "external_etf_id", columnDefinition = "CHAR(36)", nullable = false)
    private String externalEtfId;

    @Column(name = "qty_delta", nullable = false)
    private int qtyDelta;

    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "reference_id", columnDefinition = "CHAR(36)")
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
