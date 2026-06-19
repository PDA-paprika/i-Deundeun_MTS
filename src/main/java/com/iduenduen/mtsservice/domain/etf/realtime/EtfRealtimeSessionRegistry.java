package com.iduenduen.mtsservice.domain.etf.realtime;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EtfRealtimeSessionRegistry {

    private final Map<String, Set<WebSocketSession>> sessionsByCode = new ConcurrentHashMap<>();

    public void subscribe(String etfCode, WebSocketSession session) {
        sessionsByCode.computeIfAbsent(etfCode, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unsubscribeAll(WebSocketSession session) {
        sessionsByCode.values().forEach(sessions -> sessions.remove(session));
    }

    public Set<WebSocketSession> getSessions(String etfCode) {
        return sessionsByCode.getOrDefault(etfCode, Collections.emptySet());
    }
}
