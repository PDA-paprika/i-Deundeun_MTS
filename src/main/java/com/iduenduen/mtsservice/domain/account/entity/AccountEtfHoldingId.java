package com.iduenduen.mtsservice.domain.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class AccountEtfHoldingId implements Serializable {

    @Column(name = "external_etf_id", columnDefinition = "CHAR(36)", nullable = false)
    private String externalEtfId;

    @Column(name = "account_id", columnDefinition = "CHAR(36)", nullable = false)
    private String accountId;
}
