package br.com.fightConnect.infrastructure.aspects;

import br.com.fightConnect.infrastructure.streaming.LogAuditoriaEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

    private final RabbitTemplate rabbitTemplate;

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object auditAction(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        try {
            sendAuditLog(joinPoint, duration, null);
        } catch (Exception e) {
            System.err.println("⚠️ Falha ao enviar log de auditoria (Suporte): " + e.getMessage());
        }

        return result;
    }

    private void sendAuditLog(ProceedingJoinPoint joinPoint, long duration, Exception error) {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        
        HttpServletRequest request = attrs.getRequest();
        
        UUID usuarioId = null;
        UUID equipeId = null;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            usuarioId = UUID.fromString(jwt.getSubject());
            String equipeIdStr = jwt.getClaimAsString("equipeId");
            if (equipeIdStr != null) equipeId = UUID.fromString(equipeIdStr);
        }

        Map<String, Object> payload = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            payload.put("requestPayload", args[0]); 
        }

        LogAuditoriaEvent event = LogAuditoriaEvent.builder()
                .usuarioId(usuarioId)
                .equipeId(equipeId)
                .metodoHttp(request.getMethod())
                .url(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .tipo(error == null ? "SUCESSO" : "ERRO")
                .entidade("SUPORTE_TICKET")
                .descricao("Ação Suporte: " + joinPoint.getSignature().getName())
                .duracaoMs(duration)
                .dados(payload)
                .build();

        rabbitTemplate.convertAndSend("sync.fightconnect", "sync.fightconnect.logs.auditoria", event);
    }
}
