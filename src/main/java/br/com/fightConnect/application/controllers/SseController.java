package br.com.fightConnect.application.controllers;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@Tag(name = "SSE (Server-Sent Events)")
public class SseController {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public static final String TICKET_EVENT = "ticket-update";

    @Operation(summary = "Inscrever-se para notificacoes em tempo real dos tickets")
    @GetMapping(value = "/tickets", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"mensagem\":\"Conectado as notificacoes de tickets\"}"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        log.info("SSE conectado para usuario {}", userId);
        return emitter;
    }

    public void notifyTicketUpdate(String payload) {
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(TICKET_EVENT)
                        .data(payload));
            } catch (IOException e) {
                emitters.remove(entry.getKey());
                try {
                    entry.getValue().completeWithError(e);
                } catch (Exception ignored) {}
            }
        }
    }

    @PreDestroy
    public void destroy() {
        emitters.values().forEach(SseEmitter::complete);
        emitters.clear();
    }
}
