package br.com.fightConnect.domain.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fightConnect.domain.models.entities.Ticket;
import br.com.fightConnect.domain.models.entities.TicketMessage;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAdminClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAlunoClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthProfessorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotificationHelper {

    private final MailService mailService;
    private final ApiAuthAlunoClient apiAuthAlunoClient;
    private final ApiAuthProfessorClient apiAuthProfessorClient;
    private final ApiAuthAdminClient apiAuthAdminClient;

    @Transactional
    public void notifyCreated(Ticket saved) {
        if (saved == null) return;
        sendEmailToSupportOnCreate(saved);
        enqueuePushToSupportProfilesOnCreate(saved);
    }

    @Transactional
    public void notifyStatusChanged(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
        sendEmailToUserOnStatusChange(t, oldStatus, newStatus);
    }

    @Transactional
    public void notifyFinalAnswer(Ticket t, TicketStatus oldStatus, TicketStatus newStatus, TicketMessage msg) {
        sendEmailToUserOnFinalAnswer(t, oldStatus, newStatus, msg);
    }

    @Transactional
    public void notifyNewMessage(Ticket ticket, TicketMessage msg) {
        enqueuePushNewMessage(ticket, msg);
    }

    private void sendEmailToSupportOnCreate(Ticket t) {
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("ticketNumero", nullSafe(t.getNumeroExibicao()));
            params.put("titulo", nullSafe(t.getTitulo()));
            params.put("equipe", nullSafe(t.getNomeEquipe()));
            params.put("usuario", nullSafe(t.getNomeUsuario()));
            params.put("status", t.getStatus() == null ? "" : t.getStatus().name());
            params.put("categoria", t.getCategoria() == null ? "" : t.getCategoria().name());
            params.put("prioridade", t.getPrioridade() == null ? "" : t.getPrioridade().name());
            params.put("descricao", t.getDescricao() == null ? "Sem descricao." : t.getDescricao().trim());
            mailService.notifySupportNewTicketHtml(t.getEquipeId(), params);
        } catch (Exception ignored) {
            String subject = "Novo Ticket " + nullSafe(t.getNumeroExibicao());
            String body = buildSupportNewTicketText(t);
            try { mailService.notifySupportNewTicket(t.getEquipeId(), subject, body); } catch (Exception e) {}
        }
    }

    private void sendEmailToUserOnStatusChange(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
        String email = resolveUserEmail(t.getUsuarioId());
        if (isBlank(email)) return;
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("ticketNumero", nullSafe(t.getNumeroExibicao()));
            params.put("titulo", nullSafe(t.getTitulo()));
            params.put("equipe", nullSafe(t.getNomeEquipe()));
            params.put("statusAntigo", oldStatus == null ? "" : oldStatus.name());
            params.put("statusNovo", newStatus == null ? "" : newStatus.name());
            params.put("url", "/public/suporte?ticketId=" + (t.getId() == null ? "" : t.getId().toString()));
            mailService.notifyUserStatusChangeHtml(t.getEquipeId(), email, params);
        } catch (Exception ignored) {
            String subject = "Atualizacao do Ticket " + nullSafe(t.getNumeroExibicao()) + " - "
                    + (newStatus == null ? "" : newStatus.name());
            String body = buildUserStatusChangeText(t, oldStatus, newStatus);
            try { mailService.notifyUserStatusChange(t.getEquipeId(), email, subject, body); } catch (Exception e) {}
        }
    }

    private void sendEmailToUserOnFinalAnswer(Ticket t, TicketStatus oldStatus, TicketStatus newStatus, TicketMessage msg) {
        String email = resolveUserEmail(t.getUsuarioId());
        if (isBlank(email)) return;
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("ticketNumero", nullSafe(t.getNumeroExibicao()));
            params.put("titulo", nullSafe(t.getTitulo()));
            params.put("status", newStatus == null ? "" : newStatus.name());
            params.put("resposta", (msg == null || msg.getTexto() == null) ? "" : msg.getTexto().trim());
            String ticketId = t.getId() == null ? "" : t.getId().toString();
            params.put("url", "/public/suporte?ticketId=" + ticketId);
            params.put("feedbackUrl", "/api/tickets/" + ticketId + "/feedback?nota=");
            mailService.notifyUserFinalAnswerHtml(t.getEquipeId(), email, params);
        } catch (Exception ignored) {
            String subject = "Ticket " + nullSafe(t.getNumeroExibicao()) + " - " + (newStatus == null ? "" : newStatus.name());
            String body = buildUserFinalAnswerText(t, oldStatus, newStatus, msg);
            try { mailService.notifyUserStatusChange(t.getEquipeId(), email, subject, body); } catch (Exception e) {}
        }
    }

    private void enqueuePushToSupportProfilesOnCreate(Ticket saved) {
        if (saved == null) return;
        try {
            String ticketNumero = nullSafe(saved.getNumeroExibicao());
            String ticketId = saved.getId() == null ? "" : saved.getId().toString();
            String ticketTitulo = nullSafe(saved.getTitulo());
            String usuarioNome = nullSafe(saved.getNomeUsuario());

            String titulo = "Novo Ticket " + ticketNumero;
            String corpo = usuarioNome + " abriu um ticket: " + ticketTitulo;
            String url = "/public/suporte" + (ticketId.isBlank() ? "" : ("?ticketId=" + ticketId));
            String tag = "suporte-ticket-" + (ticketId.isBlank() ? ticketNumero : ticketId);

            mailService.notifyPushToProfiles(saved.getEquipeId(), List.of("SUPER_GESTOR", "SUPER_ADMIN"),
                    "SUPORTE_TICKET_CRIADO", titulo, corpo,
                    url, tag, ticketId, ticketNumero, ticketTitulo, usuarioNome, null);
        } catch (Exception e) {
            log.error("Falha ao enfileirar PUSH do ticket {}: {}", saved.getId(), e.getMessage());
        }
    }

    private void enqueuePushNewMessage(Ticket ticket, TicketMessage msg) {
        if (ticket == null || msg == null) return;

        final String ticketNumero = nullSafe(ticket.getNumeroExibicao());
        final String ticketId = (ticket.getId() == null) ? "" : ticket.getId().toString();
        final String ticketTitulo = nullSafe(ticket.getTitulo());
        final UUID autorId = msg.getAutorUsuarioId();
        final UUID donoId = ticket.getUsuarioId();
        final boolean autorEhDono = autorId != null && donoId != null && autorId.equals(donoId);
        final String autorNome = nullSafe(msg.getAutorNome());

        String preview = nullSafe(msg.getTexto()).replace("\n", " ").trim();
        if (preview.length() > 110) preview = preview.substring(0, 110) + "...";
        final String titulo = "Nova mensagem - Ticket " + ticketNumero;
        final String corpo = preview;
        final String url = "/public/suporte" + (ticketId.isBlank() ? "" : ("?ticketId=" + ticketId));
        final String tag = "suporte-ticket-" + (ticketId.isBlank() ? ticketNumero : ticketId);

        try {
            if (autorEhDono) {
                mailService.notifyPushToProfiles(ticket.getEquipeId(),
                        List.of("SUPER_GESTOR", "SUPER_ADMIN"),
                        "SUPORTE_NOVA_MENSAGEM", titulo, corpo, url, tag,
                        ticketId, ticketNumero, ticketTitulo, autorNome, null);
                return;
            }
            mailService.notifyPushToUser(ticket.getEquipeId(), donoId, "SUPORTE_NOVA_MENSAGEM",
                    titulo, corpo, url, tag, ticketId, ticketNumero, ticketTitulo, autorNome, null);
        } catch (Exception e) {
            log.error("Falha ao enfileirar PUSH nova mensagem ticketId={}", ticket.getId(), e);
        }
    }

    private String resolveUserEmail(UUID usuarioId) {
        try {
            var info = buscarInfoUsuario(usuarioId);
            return info != null ? info.email() : null;
        } catch (Exception ignored) { return null; }
    }

    private UsuarioInfo buscarInfoUsuario(UUID usuarioId) {
        try {
            var a = apiAuthAlunoClient.buscarPorId(usuarioId);
            if (a != null) return new UsuarioInfo(a.getNome(), a.getEmail());
        } catch (Exception ignored) {}
        try {
            var p = apiAuthProfessorClient.buscarPorId(usuarioId);
            if (p != null) return new UsuarioInfo(p.getNome(), p.getEmail());
        } catch (Exception ignored) {}
        try {
            var u = apiAuthAdminClient.buscarPorId(usuarioId);
            if (u != null) return new UsuarioInfo(u.getNome(), u.getEmail());
        } catch (Exception ignored) {}
        return new UsuarioInfo("Usuario", null);
    }

    private record UsuarioInfo(String nome, String email) {}

    private String buildSupportNewTicketText(Ticket t) {
        return """
                FightConnect - Novo Ticket

                Ticket: %s
                Titulo: %s
                Equipe: %s
                Usuario: %s
                Status: %s
                Categoria: %s
                Prioridade: %s

                Descricao:
                %s

                - Enviado automaticamente pelo sistema FightConnect.
                """.formatted(
                        nullSafe(t.getNumeroExibicao()),
                        nullSafe(t.getTitulo()),
                        nullSafe(t.getNomeEquipe()),
                        nullSafe(t.getNomeUsuario()),
                        t.getStatus() == null ? "" : t.getStatus().name(),
                        t.getCategoria() == null ? "" : t.getCategoria().name(),
                        t.getPrioridade() == null ? "" : t.getPrioridade().name(),
                        t.getDescricao() == null ? "Sem descricao." : t.getDescricao().trim());
    }

    private String buildUserStatusChangeText(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
        return """
                FightConnect - Suporte

                Seu ticket foi atualizado.

                Ticket: %s
                Titulo: %s
                Equipe: %s
                Status anterior: %s
                Novo status: %s

                - Enviado automaticamente pelo sistema FightConnect.
                """.formatted(
                        nullSafe(t.getNumeroExibicao()),
                        nullSafe(t.getTitulo()),
                        nullSafe(t.getNomeEquipe()),
                        oldStatus == null ? "" : oldStatus.name(),
                        newStatus == null ? "" : newStatus.name());
    }

    private String buildUserFinalAnswerText(Ticket t, TicketStatus oldStatus, TicketStatus newStatus, TicketMessage msg) {
        String resposta = (msg == null || msg.getTexto() == null) ? "" : msg.getTexto().trim();
        if (resposta.isBlank()) resposta = "Sem detalhes.";

        return """
                FightConnect - Suporte

                Seu ticket foi finalizado.

                Ticket: %s
                Titulo: %s
                Equipe: %s
                Novo status: %s

                Resposta do suporte:
                %s

                - Enviado automaticamente pelo sistema FightConnect.
                """.formatted(
                        nullSafe(t.getNumeroExibicao()),
                        nullSafe(t.getTitulo()),
                        nullSafe(t.getNomeEquipe()),
                        newStatus == null ? "" : newStatus.name(),
                        resposta);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
