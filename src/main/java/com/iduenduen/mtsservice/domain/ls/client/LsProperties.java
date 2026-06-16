package com.iduenduen.mtsservice.domain.ls.client;

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
}
