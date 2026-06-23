package com.iduenduen.mtsservice.domain.order.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExecutionWebSocketHandler extends TextWebSocketHandler {

    private final AccountSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CONNECTED");
        payload.put("channel", "ORDER_EXECUTION");
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = objectMapper.readTree(message.getPayload());
        if (!"subscribe".equals(node.path("action").asText())) {
            sendError(session, "Unsupported action.");
            return;
        }

        long accountId = node.path("account_id").asLong(0);
        if (accountId == 0) {
            sendError(session, "account_id is required.");
            return;
        }

        registry.subscribe(accountId, session);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "SUBSCRIBED");
        payload.put("channel", "ORDER_EXECUTION");
        payload.put("account_id", accountId);
        session.sendMessage(new TextMessage(payload.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unsubscribeAll(session);
    }

    public void pushExecution(Long accountId, Long orderId, String etfCode, String side, int filledQty, long execPrice) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "EXECUTION");
        payload.put("order_id", orderId);
        payload.put("etf_code", etfCode);
        payload.put("side", side);
        payload.put("filled_qty", filledQty);
        payload.put("exec_price", execPrice);

        TextMessage msg = new TextMessage(payload.toString());
        for (WebSocketSession session : registry.getSessions(accountId)) {
            try {
                if (session.isOpen()) session.sendMessage(msg);
            } catch (IOException e) {
                log.warn("Failed to push execution. sessionId={}", session.getId(), e);
            }
        }
    }

    private void sendError(WebSocketSession session, String reason) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "ERROR");
        payload.put("reason", reason);
        session.sendMessage(new TextMessage(payload.toString()));
    }
}