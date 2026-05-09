package br.com.fightConnect.application.controllers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/dashboard/suporte", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Dashboard")
public class DashboardController {

    private final JdbcTemplate jdbc;

    @Operation(summary = "Metricas do dashboard de suporte")
    @GetMapping
    public Map<String, Object> metricas(
            @RequestParam(required = false) UUID equipeId
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("ticketsAbertos", equipeId != null
                ? count("SELECT COUNT(*) FROM tickets WHERE status IN ('ABERTO','EM_ANDAMENTO','AGUARDANDO_USUARIO') AND equipeId = ?", equipeId)
                : count("SELECT COUNT(*) FROM tickets WHERE status IN ('ABERTO','EM_ANDAMENTO','AGUARDANDO_USUARIO')"));

        result.put("ticketsFechadosMes", equipeId != null
                ? count("SELECT COUNT(*) FROM tickets WHERE status IN ('FECHADO','RESOLVIDO') AND criado_em >= date_trunc('month', CURRENT_DATE) AND equipeId = ?", equipeId)
                : count("SELECT COUNT(*) FROM tickets WHERE status IN ('FECHADO','RESOLVIDO') AND criado_em >= date_trunc('month', CURRENT_DATE)"));

        result.put("ticketsTotal", equipeId != null
                ? count("SELECT COUNT(*) FROM tickets WHERE equipeId = ?", equipeId)
                : count("SELECT COUNT(*) FROM tickets"));

        result.put("ticketsPorStatus", equipeId != null
                ? jdbc.queryForList("SELECT status, COUNT(*) as total FROM tickets WHERE equipeId = ? GROUP BY status ORDER BY total DESC", equipeId)
                : jdbc.queryForList("SELECT status, COUNT(*) as total FROM tickets GROUP BY status ORDER BY total DESC"));

        result.put("ticketsPorCategoria", equipeId != null
                ? jdbc.queryForList("SELECT COALESCE(categoria, 'SEM_CATEGORIA') as categoria, COUNT(*) as total FROM tickets WHERE equipeId = ? GROUP BY categoria ORDER BY total DESC", equipeId)
                : jdbc.queryForList("SELECT COALESCE(categoria, 'SEM_CATEGORIA') as categoria, COUNT(*) as total FROM tickets GROUP BY categoria ORDER BY total DESC"));

        result.put("ticketsPorDia", equipeId != null
                ? jdbc.queryForList("SELECT DATE(criado_em) as dia, COUNT(*) as total FROM tickets WHERE equipeId = ? GROUP BY DATE(criado_em) ORDER BY dia DESC LIMIT 30", equipeId)
                : jdbc.queryForList("SELECT DATE(criado_em) as dia, COUNT(*) as total FROM tickets GROUP BY DATE(criado_em) ORDER BY dia DESC LIMIT 30"));

        result.put("tempoMedioRespostaMinutos", equipeId != null
                ? jdbc.queryForObject("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (primeira_resposta_em - criado_em))/60), 0) FROM tickets WHERE primeira_resposta_em IS NOT NULL AND equipeId = ?", Double.class, equipeId)
                : jdbc.queryForObject("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (primeira_resposta_em - criado_em))/60), 0) FROM tickets WHERE primeira_resposta_em IS NOT NULL", Double.class));

        result.put("tempoMedioFechamentoHoras", equipeId != null
                ? jdbc.queryForObject("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (fechado_em - criado_em))/3600), 0) FROM tickets WHERE fechado_em IS NOT NULL AND equipeId = ?", Double.class, equipeId)
                : jdbc.queryForObject("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (fechado_em - criado_em))/3600), 0) FROM tickets WHERE fechado_em IS NOT NULL", Double.class));

        result.put("satisfacaoMedia", equipeId != null
                ? jdbc.queryForObject("SELECT COALESCE(AVG(nota), 0) FROM ticket_feedback tf JOIN tickets t ON tf.ticket_id = t.id WHERE t.equipeId = ?", Double.class, equipeId)
                : jdbc.queryForObject("SELECT COALESCE(AVG(nota), 0) FROM ticket_feedback tf JOIN tickets t ON tf.ticket_id = t.id", Double.class));

        result.put("reaberturas", equipeId != null
                ? count("SELECT COUNT(*) FROM tickets WHERE reabertura_seq > 0 AND equipeId = ?", equipeId)
                : count("SELECT COUNT(*) FROM tickets WHERE reabertura_seq > 0"));

        return result;
    }

    private long count(String sql) {
        Long val = jdbc.queryForObject(sql, Long.class);
        return val != null ? val : 0;
    }

    private long count(String sql, Object... args) {
        Long val = jdbc.queryForObject(sql, Long.class, args);
        return val != null ? val : 0;
    }
}
