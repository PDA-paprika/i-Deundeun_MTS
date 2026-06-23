package com.iduenduen.mtsservice.domain.order.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AccountSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> sessionsByAccountId = new ConcurrentHashMap<>();

    public void subscribe(Long accountId, WebSocketSession session) {
        sessionsByAccountId.computeIfAbsent(accountId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unsubscribeAll(WebSocketSession session) {
        sessionsByAccountId.values().forEach(sessions -> sessions.remove(session));
    }

    public Set<WebSocketSession> getSessions(Long accountId) {
        return sessionsByAccountId.getOrDefault(accountId, Collections.emptySet());
    }
}