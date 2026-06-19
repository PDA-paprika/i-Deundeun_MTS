package com.iduenduen.mtsservice.domain.etf.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
public class EtfPriceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EtfRealtimeSessionRegistry registry = new EtfRealtimeSessionRegistry();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CONNECTED");
        payload.put("channel", "PRICE");
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = objectMapper.readTree(message.getPayload());
        if (!"subscribe".equals(node.path("action").asText())) {
            sendError(session, "Unsupported action.");
            return;
        }
        if (!node.path("etf_codes").isArray()) {
            sendError(session, "etf_codes must be an array.");
            return;
        }

        ArrayNode codes = (ArrayNode) node.path("etf_codes");
        for (JsonNode code : codes) {
            registry.subscribe(code.asText(), session);
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "SUBSCRIBED");
        payload.put("channel", "PRICE");
        payload.set("etf_codes", codes);
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unsubscribeAll(session);
    }

    public void broadcastPriceUpdate(String etfCode, long currentPrice, long priceChange, double changeRate, long volume) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "PRICE_UPDATE");
        payload.put("etf_code", etfCode);
        payload.put("current_price", currentPrice);
        payload.put("price_change", priceChange);
        payload.put("change_rate", changeRate);
        payload.put("volume", volume);
        payload.put("timestamp", LocalDateTime.now().toString());

        TextMessage message = new TextMessage(payload.toString());
        for (WebSocketSession session : registry.getSessions(etfCode)) {
            sendQuietly(session, message);
        }
    }

    private void sendQuietly(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.warn("Failed to push price update. sessionId={}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String reason) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "ERROR");
        payload.put("reason", reason);
        session.sendMessage(new TextMessage(payload.toString()));
    }
}
