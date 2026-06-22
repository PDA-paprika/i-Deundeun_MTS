package com.iduenduen.mtsservice.domain.etf.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
public class EtfCandleWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EtfRealtimeSessionRegistry registry = new EtfRealtimeSessionRegistry();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CONNECTED");
        payload.put("channel", "CANDLE");
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = objectMapper.readTree(message.getPayload());
        if (!"subscribe".equals(node.path("action").asText())) {
            sendError(session, "Unsupported action.");
            return;
        }
        String etfCode = node.path("etf_code").asText();
        String interval = node.path("interval").asText();
        if (etfCode.isBlank() || interval.isBlank()) {
            sendError(session, "etf_code and interval are required.");
            return;
        }

        registry.subscribe(subscriptionKey(etfCode, interval), session);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "SUBSCRIBED");
        payload.put("channel", "CANDLE");
        payload.put("etf_code", etfCode);
        payload.put("interval", interval);
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unsubscribeAll(session);
    }

    public void broadcastCandleUpdate(String etfCode, String interval, long candleTimeEpochSeconds,
                                       long open, long high, long low, long close, long volume) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CANDLE_UPDATE");
        payload.put("etf_code", etfCode);
        payload.put("interval", interval);
        payload.put("candle_time", candleTimeEpochSeconds);
        payload.put("open", open);
        payload.put("high", high);
        payload.put("low", low);
        payload.put("close", close);
        payload.put("volume", volume);

        TextMessage message = new TextMessage(payload.toString());
        for (WebSocketSession session : registry.getSessions(subscriptionKey(etfCode, interval))) {
            sendQuietly(session, message);
        }
    }

    private String subscriptionKey(String etfCode, String interval) {
        return etfCode + ":" + interval;
    }

    private void sendQuietly(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.warn("Failed to push candle update. sessionId={}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String reason) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "ERROR");
        payload.put("reason", reason);
        session.sendMessage(new TextMessage(payload.toString()));
    }
}
