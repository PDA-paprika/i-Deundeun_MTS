package com.iduenduen.mtsservice.common.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CoreTradeRequest {

    @JsonProperty("account_id")
    private Long accountId;

    @JsonProperty("parent_id")
    private Long parentId;

    @JsonProperty("etf_id")
    private Long etfId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("etf_name")
    private String etfName;

    private Integer qty;

    private Long price;

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("reference_type")
    private String referenceType;

    @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    @JsonProperty("occurred_at")
    private LocalDateTime occurredAt;

    @JsonProperty("child_id")
    private Long childId;

    @JsonProperty("goal_id")
    private Long goalId;

    @JsonProperty("link_id")
    private Long linkId;
}