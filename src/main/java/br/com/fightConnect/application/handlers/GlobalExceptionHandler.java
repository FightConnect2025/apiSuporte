package br.com.fightConnect.application.handlers;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import br.com.fightConnect.infrastructure.messaging.AuditoriaProducer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditoriaProducer auditoriaProducer;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        registrarErro("ResponseStatusException", ex.getReason(), request, status);
        return buildErrorResponse(status, ex.getReason(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        registrarErro("IllegalArgumentException", ex.getMessage(), request, HttpStatus.BAD_REQUEST);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex, HttpServletRequest request) {
        registrarErro(ex.getClass().getSimpleName(), ex.getMessage(), request, HttpStatus.INTERNAL_SERVER_ERROR);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado.", request.getRequestURI());
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String mensagem, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("mensagem", mensagem);
        body.put("path", path);
        return new ResponseEntity<>(body, status);
    }

    private void registrarErro(String entidade, String descricao, HttpServletRequest request, HttpStatus status) {
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) correlationId = UUID.randomUUID().toString();

            String usuarioId = null;
            String equipeId = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                usuarioId = jwt.getSubject();
                equipeId = jwt.getClaimAsString("equipeId");
            }

            Map<String, Object> detalhes = new java.util.HashMap<>();
            detalhes.put("correlationId", correlationId);
            detalhes.put("path", request.getRequestURI());
            detalhes.put("mensagem", descricao);
            detalhes.put("status", status.value());
            detalhes.put("ip", getIp(request));
            detalhes.put("userAgent", request.getHeader("User-Agent"));
            detalhes.put("usuarioId", usuarioId);
            detalhes.put("equipeId", equipeId);
            detalhes.put("timestamp", LocalDateTime.now().toString());

            auditoriaProducer.enviarEvento(
                "ERROR",
                entidade,
                safeUuid(usuarioId),
                "Erro capturado na API Suporte",
                detalhes
            );
        } catch (Exception e) {
            System.err.println("⚠️ Falha ao enviar log de erro de auditoria (Suporte): " + e.getMessage());
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private UUID safeUuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
