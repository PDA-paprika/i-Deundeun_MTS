package com.iduenduen.mtsservice.common.ls.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ls.api")
public class LsProperties {

    private String appKey;
    private String appSecret;
    private String baseUrl;
    private String wsUrl;
    private String macAddress;
    private String accountNumber;  // SC1 체결 통보 구독용 계좌번호
}
