package com.safehome.safehome_api.domain.alert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    // userId → SseEmitter
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(UUID userId) {
        // 타임아웃 30분
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitter.onCompletion(() -> remove(userId));
        emitter.onTimeout(()     -> remove(userId));
        emitter.onError(e        -> remove(userId));

        emitters.put(userId, emitter);
        log.info("[SSE] 연결됨 userId={}", userId);

        // 연결 확인용 초기 이벤트
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            remove(userId);
        }

        return emitter;
    }

    public void send(UUID userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event().name("alert").data(data));
        } catch (IOException e) {
            log.warn("[SSE] 전송 실패 userId={}", userId);
            remove(userId);
        }
    }

    public void broadcast(Object data) {
        emitters.forEach((userId, emitter) -> send(userId, data));
    }

    public void remove(UUID userId) {
        emitters.remove(userId);
        log.info("[SSE] 연결 해제 userId={}", userId);
    }

    public int getConnectionCount() {
        return emitters.size();
    }
}