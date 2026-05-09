package br.com.fightConnect.domain.services;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fightConnect.domain.models.entities.WebhookConfig;
import br.com.fightConnect.infrastructure.repositories.WebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final ObjectMapper objectMapper;

    public void disparar(String evento, Map<String, Object> payload) {
        UUID equipeId = payload.get("equipeId") != null
                ? java.util.UUID.fromString(payload.get("equipeId").toString())
                : null;
        if (equipeId == null) return;

        var configs = webhookConfigRepository.findByEquipeIdAndAtivoTrue(equipeId);

        for (WebhookConfig config : configs) {
            if (!config.getEventos().contains(evento)) continue;

            try {
                String body = objectMapper.writeValueAsString(payload);
                java.net.URI uri = new java.net.URI(config.getUrl());

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Content-Type", "application/json")
                        .header("X-Event-Type", evento)
                        .header("X-Webhook-Signature", gerarAssinatura(body, config.getSegredo()))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();

                client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding())
                        .thenAccept(r -> {
                            if (r.statusCode() >= 400) {
                                log.warn("Webhook {} retornou status {} para evento {}", config.getUrl(), r.statusCode(), evento);
                            }
                        })
                        .exceptionally(e -> {
                            log.error("Falha ao enviar webhook {} para evento {}: {}", config.getUrl(), evento, e.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                log.error("Erro ao preparar webhook {}: {}", config.getUrl(), e.getMessage());
            }
        }
    }

    private String gerarAssinatura(String body, String segredo) {
        if (segredo == null || segredo.isBlank()) return "";
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(
                    segredo.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
