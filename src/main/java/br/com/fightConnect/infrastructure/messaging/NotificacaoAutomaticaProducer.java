package br.com.fightConnect.infrastructure.messaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import br.com.fightConnect.domain.models.dtos.NotificacaoAutomaticaRequestDto;
import br.com.fightConnect.domain.models.enums.TipoCanal;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificacaoAutomaticaProducer {

    public static final String FILA_NOTIFICACAO = "notificacao.automatica";

    private final RabbitTemplate rabbitTemplate;
    private static final Logger log = LoggerFactory.getLogger(NotificacaoAutomaticaProducer.class);

    /**
     * ✅ base
     *
     * - Email continua funcionando.
     * - Push: envia como MAP com "eventType" no ROOT (camelCase),
     *   pra não depender do converter/naming strategy.
     */
    public void enviar(NotificacaoAutomaticaRequestDto dto) {
        String corrId = UUID.randomUUID().toString();

        try {
            Map<String, Object> msg = new LinkedHashMap<>();

            // ROOT fields
            msg.put("equipeId", dto.getEquipeId() == null ? null : dto.getEquipeId().toString());
            msg.put("tipo", dto.getTipo());
            msg.put("canal", dto.getCanal() == null ? null : dto.getCanal().name());
            msg.put("destinatario", dto.getDestinatario());

            // ✅ CHAVE CRÍTICA: worker precisa disso no root
            msg.put("eventType", dto.getEventType());

            msg.put("titulo", dto.getTitulo());
            msg.put("corpo", dto.getCorpo());
            msg.put("url", dto.getUrl());
            msg.put("tag", dto.getTag());

            // extras suporte
            msg.put("ticketId", dto.getTicketId());
            msg.put("ticketNumero", dto.getTicketNumero());
            msg.put("ticketTitulo", dto.getTicketTitulo());
            msg.put("usuarioNome", dto.getUsuarioNome());
            msg.put("status", dto.getStatus());

            // email fields (compat)
            msg.put("assunto", dto.getAssunto());
            msg.put("mensagemHtml", dto.getMensagemHtml());
            msg.put("mensagemResumo", dto.getMensagemResumo());

            // log mínimo (sem JSON)
            log.info("📤 RabbitMQ SEND: fila={} corrId={} tipo={} canal={} eventType={} destinatarios={}",
                    FILA_NOTIFICACAO,
                    corrId,
                    dto.getTipo(),
                    dto.getCanal(),
                    dto.getEventType(),
                    dto.getDestinatario() == null ? 0 : dto.getDestinatario().size()
            );

            rabbitTemplate.convertAndSend(FILA_NOTIFICACAO, msg);

            log.info("✅ RabbitMQ SEND: enviado para fila={} corrId={}", FILA_NOTIFICACAO, corrId);

        } catch (Exception e) {
            log.error("❌ RabbitMQ SEND: falhou ao publicar (fila={}, corrId={}) -> {}",
                    FILA_NOTIFICACAO, corrId, e.getMessage(), e);
        }
    }

    // ✅ compatível com o que você já usa hoje (não quebra nada)
    public void enviarEmail(UUID equipeId, String email, String mensagemHtml) {
        enviarEmail(equipeId, email, null, mensagemHtml);
    }

    // ✅ NOVO: manda subject separado (o worker usa subject certo)
    public void enviarEmail(UUID equipeId, String email, String subject, String mensagemHtml) {
        var dto = new NotificacaoAutomaticaRequestDto();
        dto.setEquipeId(equipeId);
        dto.setTipo("SUPORTE");
        dto.setCanal(TipoCanal.EMAIL);
        dto.setDestinatario(List.of(email));

        dto.setAssunto(subject);
        dto.setMensagemHtml(mensagemHtml);
        dto.setMensagemResumo(stripHtml(mensagemHtml));

        enviar(dto);
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
