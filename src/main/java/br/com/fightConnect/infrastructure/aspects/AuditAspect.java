package br.com.fightConnect.infrastructure.aspects;

import br.com.fightConnect.infrastructure.audit.AuditSensitiveGet;
import br.com.fightConnect.infrastructure.audit.AuditContextEnricher;
import br.com.fightConnect.infrastructure.audit.PayloadSanitizer;
import br.com.fightConnect.infrastructure.streaming.LogAuditoriaEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final long WARNING_DURATION_MS = 2000;

    private final RabbitTemplate rabbitTemplate;
    private final PayloadSanitizer payloadSanitizer;
    private final AuditContextEnricher contextEnricher;

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object auditMutationAction(ProceedingJoinPoint joinPoint) throws Throwable {
        return executeWithAudit(joinPoint, "SUPORTE_TICKET");
    }

    @Around("@annotation(br.com.fightConnect.infrastructure.audit.AuditSensitiveGet)")
    public Object auditSensitiveGet(ProceedingJoinPoint joinPoint) throws Throwable {
        return executeWithAudit(joinPoint, "LEITURA_SENSIVEL");
    }

    private Object executeWithAudit(ProceedingJoinPoint joinPoint, String contexto) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                sendAuditLog(joinPoint, duration, error, result, contexto);
            } catch (Exception e) {
                System.err.println("⚠️ Falha ao enviar log de auditoria (Suporte): " + e.getMessage());
            }
        }
    }

    private void sendAuditLog(ProceedingJoinPoint joinPoint, long duration, Exception error, Object result, String contexto) {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();

        String usuarioId = null;
        String equipeId = null;
        String nomeUsuario = null;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            usuarioId = jwt.getSubject();
            String equipeIdStr = jwt.getClaimAsString("equipeId");
            if (equipeIdStr != null) equipeId = equipeIdStr;
            nomeUsuario = jwt.getClaimAsString("nome");
            if (nomeUsuario == null) nomeUsuario = jwt.getClaimAsString("email");
        }

        String correlationId = MDC.get("correlationId");
        if (correlationId == null) correlationId = UUID.randomUUID().toString();

        String nivelLog = error != null ? "ERROR" : "INFO";
        String warningType = null;

        if (duration > WARNING_DURATION_MS) {
            warningType = "LENTO";
            nivelLog = "WARNING";
        }

        if (error != null && isUnauthorizedError(error)) {
            warningType = "ACESSO_NEGADO";
            nivelLog = "WARNING";
        }

        Map<String, Object> payload = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] != null) {
            payload.put("requestPayload", payloadSanitizer.sanitize(args[0]));
        }

        if (result != null && error == null) {
            payload = contextEnricher.enrichWithEntity("SUPORTE_TICKET", result, payload);
        }

        Map<String, String> headers = extractImportantHeaders(request);

        String descricao = error != null ? "ERRO: " + error.getMessage() : "Ação Suporte: " + joinPoint.getSignature().getName();

        LogAuditoriaEvent event = LogAuditoriaEvent.builder()
                .correlationId(correlationId)
                .usuarioId(usuarioId != null ? UUID.fromString(usuarioId) : null)
                .equipeId(equipeId != null ? UUID.fromString(equipeId) : null)
                .nomeUsuario(nomeUsuario)
                .metodoHttp(request.getMethod())
                .url(request.getRequestURI())
                .ip(getIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .tipo(error == null ? "SUCESSO" : "ERRO")
                .nivelLog(nivelLog)
                .warningType(warningType)
                .entidade("SUPORTE_TICKET")
                .descricao(descricao)
                .duracaoMs(duration)
                .dados(payload)
                .headers(headers)
                .className(joinPoint.getTarget().getClass().getSimpleName())
                .methodName(joinPoint.getSignature().getName())
                .stackTrace(error != null ? getStackTrace(error) : null)
                .build();

        rabbitTemplate.convertAndSend("sync.fightconnect", "sync.fightconnect.logs.auditoria", event);
    }

    private Map<String, String> extractImportantHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        String[] importantHeaders = {"Origin", "Referer", "Accept-Language", "Content-Type"};
        for (String header : importantHeaders) {
            String value = request.getHeader(header);
            if (value != null) headers.put(header, value);
        }
        return headers;
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean isUnauthorizedError(Exception error) {
        return error.getClass().getSimpleName().contains("AccessDenied") ||
               error.getClass().getSimpleName().contains("Unauthorized");
    }

    private String getStackTrace(Exception e) {
        StackTraceElement[] stack = e.getStackTrace();
        if (stack.length == 0) return e.getMessage();
        StringBuilder sb = new StringBuilder();
        int maxLines = Math.min(10, stack.length);
        for (int i = 0; i < maxLines; i++) {
            sb.append(stack[i].toString());
            if (i < maxLines - 1) sb.append(" | ");
        }
        return sb.toString();
    }
}
