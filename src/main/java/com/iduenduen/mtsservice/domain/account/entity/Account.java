package com.iduenduen.mtsservice.domain.account.entity;

import com.iduenduen.mtsservice.domain.account.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @Column(name = "account_id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "child_id", columnDefinition = "CHAR(36)")
    private String childId;

    @Column(name = "parent_id", columnDefinition = "CHAR(36)")
    private String parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 10, nullable = false)
    private AccountType accountType;

    @Column(name = "account_number", length = 20, nullable = false)
    private String accountNumber;

    @Column(name = "available_amt", nullable = false)
    private Long availableAmt = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
