package br.com.fightConnect.domain.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.support-to}")
    private String supportTo;

    public void sendText(UUID equipeId, String to, String text) {
        if (isBlank(to)) return;
        producer.enviarEmail(equipeId, to.trim(), text == null ? "" : text);
    }

    public void sendHtml(UUID equipeId, String to, String subject, String html) {
        if (isBlank(to)) return;
        var dto = new NotificacaoAutomaticaRequestDto();
        dto.setEquipeId(equipeId);
        dto.setTipo(TIPO_SUPORTE);
        dto.setCanal(TipoCanal.EMAIL);
        dto.setDestinatario(List.of(to.trim()));
        dto.setAssunto(subject);
        dto.setMensagemHtml(html);
        dto.setMensagemResumo(stripHtml(html));
        producer.enviar(dto);
    }

    public void notifySupportNewTicket(UUID equipeId, String subject, String textBody) {
        if (isBlank(supportTo)) return;
        String payload = buildPayload(subject, textBody);
        sendText(equipeId, supportTo, payload);
    }

    public void notifySupportNewTicketHtml(UUID equipeId, Map<String, Object> params) {
        if (isBlank(supportTo)) return;
        String html = renderTemplate("email/novo-ticket", params);
        String subject = "Novo Ticket " + params.getOrDefault("ticketNumero", "");
        sendHtml(equipeId, supportTo, subject, html);
    }

    public void notifyUserStatusChangeHtml(UUID equipeId, String userEmail, Map<String, Object> params) {
        if (isBlank(userEmail)) return;
        String html = renderTemplate("email/atualizacao-status", params);
        String subject = "Atualizacao do Ticket " + params.getOrDefault("ticketNumero", "");
        sendHtml(equipeId, userEmail, subject, html);
    }

    public void notifyUserFinalAnswerHtml(UUID equipeId, String userEmail, Map<String, Object> params) {
        if (isBlank(userEmail)) return;
        String html = renderTemplate("email/resposta-final", params);
        String subject = "Ticket " + params.getOrDefault("ticketNumero", "") + " - " + params.getOrDefault("status", "");
        sendHtml(equipeId, userEmail, subject, html);
    }

    public void notifyUserStatusChange(UUID equipeId, String userEmail, String subject, String textBody) {
        if (isBlank(userEmail)) return;
        String payload = buildPayload(subject, textBody);
        sendText(equipeId, userEmail, payload);
    }

    public void notifyPushToProfile(
            UUID equipeId, String perfil, String eventType, String titulo, String corpo,
            String url, String tag, String ticketId, String ticketNumero,
            String ticketTitulo, String usuarioNome, String status
    ) {
        if (equipeId == null || isBlank(perfil)) return;

        NotificacaoAutomaticaRequestDto dto = basePushDto(
                equipeId, eventType, titulo, corpo, url, tag,
                ticketId, ticketNumero, ticketTitulo, usuarioNome, status
        );
        dto.setDestinatario(List.of(perfil.trim()));
        producer.enviar(dto);
    }

    public void notifyPushToUser(
            UUID equipeId, UUID userId, String eventType, String titulo, String corpo,
            String url, String tag, String ticketId, String ticketNumero,
            String ticketTitulo, String usuarioNome, String status
    ) {
        if (equipeId == null || userId == null) return;

        NotificacaoAutomaticaRequestDto dto = basePushDto(
                equipeId, eventType, titulo, corpo, url, tag,
                ticketId, ticketNumero, ticketTitulo, usuarioNome, status
        );
        dto.setDestinatario(List.of(userId.toString()));
        producer.enviar(dto);
    }

    public void notifyPushToProfiles(
            UUID equipeId, List<String> perfis, String eventType, String titulo, String corpo,
            String url, String tag, String ticketId, String ticketNumero,
            String ticketTitulo, String usuarioNome, String status
    ) {
        if (equipeId == null || perfis == null || perfis.isEmpty()) return;

        NotificacaoAutomaticaRequestDto dto = basePushDto(
                equipeId, eventType, titulo, corpo, url, tag,
                ticketId, ticketNumero, ticketTitulo, usuarioNome, status
        );
        dto.setDestinatario(perfis.stream().map(String::trim).toList());
        producer.enviar(dto);
    }

    private String renderTemplate(String template, Map<String, Object> params) {
        Context context = new Context();
        params.forEach(context::setVariable);
        return templateEngine.process(template, context);
    }

    private NotificacaoAutomaticaRequestDto basePushDto(
            UUID equipeId, String eventType, String titulo, String corpo,
            String url, String tag, String ticketId, String ticketNumero,
            String ticketTitulo, String usuarioNome, String status
    ) {
        NotificacaoAutomaticaRequestDto dto = new NotificacaoAutomaticaRequestDto();
        dto.setEquipeId(equipeId);
        dto.setCanal(TipoCanal.PUSH);
        dto.setTipo(TIPO_SUPORTE);

        String et = normalizeEventType(eventType);
        dto.setEventType(et);

        String safeTitle = sanitizeTitle(defaultIfBlank(titulo, "Notificacao"));
        String safeBody  = sanitizeBody(defaultIfBlank(corpo, "Voce recebeu uma nova notificacao."));
        String safeUrl   = defaultIfBlank(url, "");
        String safeTag   = defaultIfBlank(tag, buildDefaultTag(et, ticketId));

        dto.setTitulo(safeTitle);
        dto.setCorpo(safeBody);
        dto.setUrl(safeUrl);
        dto.setTag(safeTag);

        dto.setTicketId(nullIfBlank(ticketId));
        dto.setTicketNumero(nullIfBlank(ticketNumero));
        dto.setTicketTitulo(nullIfBlank(ticketTitulo));
        dto.setUsuarioNome(nullIfBlank(usuarioNome));
        dto.setStatus(nullIfBlank(status));

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
        if (t.isBlank()) t = "Notificacao";
        if (t.length() > 80) t = t.substring(0, 77) + "...";
        return t;
    }

    private static String sanitizeBody(String corpo) {
        String c = corpo == null ? "" : corpo.replace("\r", "").trim();
        if (c.isBlank()) c = "Voce recebeu uma nova notificacao.";
        if (c.length() > 240) c = c.substring(0, 237) + "...";
        return c;
    }

    private static String buildDefaultTag(String eventType, String ticketId) {
        if (!isBlank(ticketId)) return "suporte-ticket-" + ticketId.trim();
        return defaultIfBlank(eventType, FALLBACK_EVENT) + "-" + System.currentTimeMillis();
    }

    private static String nullIfBlank(String s) { return isBlank(s) ? null : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String defaultIfBlank(String value, String fallback) { return isBlank(value) ? fallback : value; }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
