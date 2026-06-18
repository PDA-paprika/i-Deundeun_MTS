package com.iduenduen.mtsservice.domain.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class AccountEtfHoldingId implements Serializable {

    @Column(name = "external_etf_id", nullable = false)
    private String externalEtfId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;
}
