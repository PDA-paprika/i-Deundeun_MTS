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
public class EtfOrderBookWebSocketHandler extends TextWebSocketHandler {

    private static final int LEVELS = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EtfRealtimeSessionRegistry registry = new EtfRealtimeSessionRegistry();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CONNECTED");
        payload.put("channel", "ORDERBOOK");
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
        if (etfCode.isBlank()) {
            sendError(session, "etf_code is required.");
            return;
        }

        registry.subscribe(etfCode, session);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "SUBSCRIBED");
        payload.put("channel", "ORDERBOOK");
        payload.put("etf_code", etfCode);
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unsubscribeAll(session);
    }

    public void broadcastOrderBookUpdate(String etfCode, long[] askPrices, long[] askQtys, long[] bidPrices, long[] bidQtys) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "ORDERBOOK_UPDATE");
        payload.put("etf_code", etfCode);

        ArrayNode asks = payload.putArray("asks");
        ArrayNode bids = payload.putArray("bids");
        for (int i = 0; i < LEVELS; i++) {
            ObjectNode ask = asks.addObject();
            ask.put("step", i + 1);
            ask.put("price", askPrices[i]);
            ask.put("qty", askQtys[i]);

            ObjectNode bid = bids.addObject();
            bid.put("step", i + 1);
            bid.put("price", bidPrices[i]);
            bid.put("qty", bidQtys[i]);
        }
        payload.put("snapshot_at", LocalDateTime.now().toString());

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
            log.warn("Failed to push orderbook update. sessionId={}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String reason) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "ERROR");
        payload.put("reason", reason);
        session.sendMessage(new TextMessage(payload.toString()));
    }
}
