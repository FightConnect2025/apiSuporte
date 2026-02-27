package br.com.fightConnect.domain.services;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.fightConnect.domain.models.dtos.NotificacaoAutomaticaRequestDto;
import br.com.fightConnect.domain.models.enums.TipoCanal;
import br.com.fightConnect.infrastructure.messaging.NotificacaoAutomaticaProducer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final String TIPO_SUPORTE = "SUPORTE";
    private static final String FALLBACK_EVENT = "PUSH";

    private final NotificacaoAutomaticaProducer producer;

    @Value("${app.mail.support-to}")
    private String supportTo;

    // ==========================
    // EMAIL (texto)
    // ==========================
    public void sendText(UUID equipeId, String to, String text) {
        if (isBlank(to)) return;
        producer.enviarEmail(equipeId, to.trim(), text == null ? "" : text);
    }

    public void notifySupportNewTicket(UUID equipeId, String subject, String textBody) {
        if (isBlank(supportTo)) return;
        String payload = buildPayload(subject, textBody);
        sendText(equipeId, supportTo, payload);
    }

    public void notifyUserStatusChange(UUID equipeId, String userEmail, String subject, String textBody) {
        if (isBlank(userEmail)) return;
        String payload = buildPayload(subject, textBody);
        sendText(equipeId, userEmail, payload);
    }

    // ==========================
    // PUSH - PERFIL
    // ==========================
    public void notifyPushToProfile(
            UUID equipeId,
            String perfil,
            String eventType,
            String titulo,
            String corpo,
            String url,
            String tag,
            String ticketId,
            String ticketNumero,
            String ticketTitulo,
            String usuarioNome,
            String status
    ) {
        if (equipeId == null) return;
        if (isBlank(perfil)) return;

        NotificacaoAutomaticaRequestDto dto = basePushDto(
                equipeId, eventType, titulo, corpo, url, tag,
                ticketId, ticketNumero, ticketTitulo, usuarioNome, status
        );

        dto.setDestinatario(List.of(perfil.trim()));
        producer.enviar(dto);
    }

    // ==========================
    // PUSH - USUÁRIO (por userId)
    // ==========================
    public void notifyPushToUser(
            UUID equipeId,
            UUID userId,
            String eventType,
            String titulo,
            String corpo,
            String url,
            String tag,
            String ticketId,
            String ticketNumero,
            String ticketTitulo,
            String usuarioNome,
            String status
    ) {
        if (equipeId == null) return;
        if (userId == null) return;

        NotificacaoAutomaticaRequestDto dto = basePushDto(
                equipeId, eventType, titulo, corpo, url, tag,
                ticketId, ticketNumero, ticketTitulo, usuarioNome, status
        );

        dto.setDestinatario(List.of(userId.toString()));
        producer.enviar(dto);
    }

    // ==========================
    // Builders
    // ==========================
    private NotificacaoAutomaticaRequestDto basePushDto(
            UUID equipeId,
            String eventType,
            String titulo,
            String corpo,
            String url,
            String tag,
            String ticketId,
            String ticketNumero,
            String ticketTitulo,
            String usuarioNome,
            String status
    ) {
        NotificacaoAutomaticaRequestDto dto = new NotificacaoAutomaticaRequestDto();

        dto.setEquipeId(equipeId);
        dto.setCanal(TipoCanal.PUSH);
        dto.setTipo(TIPO_SUPORTE);

        // ✅ garante SEMPRE preenchido
        String et = normalizeEventType(eventType);
        dto.setEventType(et);

        String safeTitle = sanitizeTitle(defaultIfBlank(titulo, "Notificação"));
        String safeBody  = sanitizeBody(defaultIfBlank(corpo, "Você recebeu uma nova notificação."));
        String safeUrl   = defaultIfBlank(url, "");
        String safeTag   = defaultIfBlank(tag, buildDefaultTag(et, ticketId));

        dto.setTitulo(safeTitle);
        dto.setCorpo(safeBody);
        dto.setUrl(safeUrl);
        dto.setTag(safeTag);

        // extras suporte
        dto.setTicketId(nullIfBlank(ticketId));
        dto.setTicketNumero(nullIfBlank(ticketNumero));
        dto.setTicketTitulo(nullIfBlank(ticketTitulo));
        dto.setUsuarioNome(nullIfBlank(usuarioNome));
        dto.setStatus(nullIfBlank(status));

        // compat legado (se existir no worker)
        dto.setAssunto(safeTitle);
        dto.setMensagemResumo(safeBody);

        return dto;
    }

    private static String buildPayload(String subject, String body) {
        String s = subject == null ? "" : subject.trim();
        if (s.length() > 120) s = s.substring(0, 117) + "...";

        String b = body == null ? "" : body.trim();
        if (b.isBlank()) return s;
        return s + "\n\n" + b;
    }

    private static String normalizeEventType(String eventType) {
        String et = defaultIfBlank(eventType, FALLBACK_EVENT).trim();
        if (et.isBlank()) return FALLBACK_EVENT;
        return et.replace(" ", "_").toUpperCase();
    }

    private static String sanitizeTitle(String titulo) {
        String t = titulo == null ? "" : titulo.trim();
        if (t.isBlank()) t = "Notificação";
        if (t.length() > 80) t = t.substring(0, 77) + "...";
        return t;
    }

    private static String sanitizeBody(String corpo) {
        String c = corpo == null ? "" : corpo.replace("\r", "").trim();
        if (c.isBlank()) c = "Você recebeu uma nova notificação.";
        if (c.length() > 240) c = c.substring(0, 237) + "...";
        return c;
    }

    private static String buildDefaultTag(String eventType, String ticketId) {
        if (!isBlank(ticketId)) return "suporte-ticket-" + ticketId.trim();
        return defaultIfBlank(eventType, FALLBACK_EVENT) + "-" + System.currentTimeMillis();
    }

    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
