package br.com.fightConnect.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditoriaProducer {

    private final RabbitTemplate rabbitTemplate;
    private static final Logger log = LoggerFactory.getLogger(AuditoriaProducer.class);

    private static final String EXCHANGE_SYNC = "sync.fightconnect";
    private static final String ROUTING_KEY = "sync.fightconnect.logs.auditoria";

    public void enviarEvento(String tipo, String entidade, UUID usuarioId, String descricao, Map<String, Object> detalhes) {
        try {
            Map<String, Object> evento = new HashMap<>();
            evento.put("tipo", tipo);
            evento.put("entidade", entidade);
            evento.put("usuarioId", usuarioId);
            evento.put("descricao", descricao);
            evento.put("dados", detalhes);
            evento.put("criadoEm", LocalDateTime.now());

            rabbitTemplate.convertAndSend(EXCHANGE_SYNC, ROUTING_KEY, evento);
        } catch (Exception e) {
            log.error("Erro ao enviar log de auditoria (Suporte): {}", e.getMessage());
        }
    }

    public void enviarEvento(String entidade, UUID usuarioId, String descricao, Map<String, Object> detalhes) {
        enviarEvento("INDEFINIDO", entidade, usuarioId, descricao, detalhes);
    }
}
