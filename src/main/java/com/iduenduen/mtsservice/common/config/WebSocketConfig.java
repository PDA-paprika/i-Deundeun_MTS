package com.iduenduen.mtsservice.common.config;

import com.iduenduen.mtsservice.domain.etf.realtime.EtfOrderBookWebSocketHandler;
import com.iduenduen.mtsservice.domain.etf.realtime.EtfPriceWebSocketHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final EtfPriceWebSocketHandler etfPriceWebSocketHandler;
    private final EtfOrderBookWebSocketHandler etfOrderBookWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(etfPriceWebSocketHandler, "/v1/etf/price")
                .setAllowedOrigins("*");
        registry.addHandler(etfOrderBookWebSocketHandler, "/v1/etf/orderbook")
                .setAllowedOrigins("*");
    }
}
