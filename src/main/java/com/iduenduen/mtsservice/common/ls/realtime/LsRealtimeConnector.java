package com.iduenduen.mtsservice.common.ls.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.iduenduen.mtsservice.common.ls.auth.LsTokenManager;
import com.iduenduen.mtsservice.common.ls.client.LsProperties;
import com.iduenduen.mtsservice.domain.etf.realtime.EtfOrderBookWebSocketHandler;
import com.iduenduen.mtsservice.domain.etf.realtime.EtfPriceWebSocketHandler;
import com.iduenduen.mtsservice.domain.etf.seed.SolEtfCodes;
import com.iduenduen.mtsservice.domain.etf.service.EtfOrderBookService;
import com.iduenduen.mtsservice.domain.etf.service.EtfPriceService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.etf.realtime", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LsRealtimeConnector {

    private static final String TR_CD_PRICE = "I5_";
    private static final String TR_CD_ORDERBOOK = "B7_";
    private static final String TR_TYPE_SUBSCRIBE = "3";
    private static final long SUBSCRIBE_DELAY_MS = 50;
    private static final long RECONNECT_DELAY_SECONDS = 10;

    private final LsProperties lsProperties;
    private final LsTokenManager lsTokenManager;
    private final EtfPriceService etfPriceService;
    private final EtfOrderBookService etfOrderBookService;
    private final EtfPriceWebSocketHandler etfPriceWebSocketHandler;
    private final EtfOrderBookWebSocketHandler etfOrderBookWebSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${app.etf.realtime.persist-enabled:false}")
    private boolean persistEnabled;

    @PostConstruct
    public void start() {
        log.info("Starting LS realtime connector. enabled=true, persistEnabled={}, url={}", persistEnabled, lsProperties.getWsUrl());
        connect();
    }

    private void connect() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        client.execute(new LsRealtimeHandler(), lsProperties.getWsUrl())
                .whenComplete((session, error) -> {
                    if (error != null) {
                        log.error("Failed to connect LS realtime websocket. Reconnect scheduled.", error);
                        scheduleReconnect();
                    }
                });
    }

    private void scheduleReconnect() {
        scheduler.schedule(this::connect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private class LsRealtimeHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("Connected to LS realtime websocket. url={}", lsProperties.getWsUrl());
            subscribeAll(session);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
            try {
                dispatch(message.getPayload());
            } catch (Exception e) {
                log.warn("Failed to handle LS realtime message. payload={}", message.getPayload(), e);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("LS realtime websocket closed. status={}. Reconnect scheduled.", status);
            scheduleReconnect();
        }

        private void subscribeAll(WebSocketSession session) throws Exception {
            int subscriptionCount = 0;
            for (String code : SolEtfCodes.CODES) {
                session.sendMessage(new TextMessage(subscribeMessage(TR_CD_PRICE, code)));
                subscriptionCount++;
                Thread.sleep(SUBSCRIBE_DELAY_MS);
                session.sendMessage(new TextMessage(subscribeMessage(TR_CD_ORDERBOOK, code)));
                subscriptionCount++;
                Thread.sleep(SUBSCRIBE_DELAY_MS);
            }
            log.info("Sent LS realtime subscriptions. codes={}, subscriptions={}", SolEtfCodes.CODES.size(), subscriptionCount);
        }

        private String subscribeMessage(String trCd, String trKey) throws Exception {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode header = root.putObject("header");
            header.put("token", lsTokenManager.getToken());
            header.put("tr_type", TR_TYPE_SUBSCRIBE);

            ObjectNode body = root.putObject("body");
            body.put("tr_cd", trCd);
            body.put("tr_key", trKey);

            return objectMapper.writeValueAsString(root);
        }
    }

    private void dispatch(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String trCd = root.path("header").path("tr_cd").asText();
        JsonNode body = root.path("body");

        if (TR_CD_PRICE.equals(trCd)) {
            handlePrice(body);
        } else if (TR_CD_ORDERBOOK.equals(trCd)) {
            handleOrderBook(body);
        } else {
            log.debug("Ignored LS realtime message. trCd={}, payload={}", trCd, payload);
        }
    }

    private void handlePrice(JsonNode body) {
        String shcode = body.path("shcode").asText().trim();
        long price = body.path("price").asLong();
        long volume = body.path("volume").asLong();
        long change = body.path("change").asLong();

        etfPriceWebSocketHandler.broadcastPriceUpdate(shcode, price, change, 0.0, volume);
        if (!persistEnabled) {
            return;
        }

        try {
            etfPriceService.applyRealtimeTick(shcode, price, volume);
        } catch (Exception e) {
            log.warn("Failed to persist realtime price tick. shcode={}, price={}, volume={}", shcode, price, volume, e);
        }
    }

    private void handleOrderBook(JsonNode body) {
        String shcode = body.path("shcode").asText().trim();
        long[] askPrices = new long[5];
        long[] askQtys = new long[5];
        long[] bidPrices = new long[5];
        long[] bidQtys = new long[5];

        for (int i = 0; i < 5; i++) {
            int step = i + 1;
            askPrices[i] = body.path("offerho" + step).asLong();
            askQtys[i] = body.path("offerrem" + step).asLong();
            bidPrices[i] = body.path("bidho" + step).asLong();
            bidQtys[i] = body.path("bidrem" + step).asLong();
        }

        etfOrderBookWebSocketHandler.broadcastOrderBookUpdate(shcode, askPrices, askQtys, bidPrices, bidQtys);
        if (!persistEnabled) {
            return;
        }

        try {
            etfOrderBookService.applyRealtimeOrderBook(shcode, askPrices, askQtys, bidPrices, bidQtys);
        } catch (Exception e) {
            log.warn("Failed to persist realtime orderbook. shcode={}", shcode, e);
        }
    }
}
