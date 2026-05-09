package br.com.fightConnect.infrastructure.streaming;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class LogAuditoriaEvent {
    private String correlationId;
    private UUID usuarioId;
    private UUID equipeId;
    private String nomeUsuario;
    private String tipo;
    private String nivelLog;
    private String warningType;
    private String entidade;
    private String referenciaId;
    private String descricao;
    private String metodoHttp;
    private String url;
    private String ip;
    private String userAgent;
    private String sessionId;
    private String tenantId;
    private Long duracaoMs;
    private Map<String, Object> dados;
    private Map<String, String> headers;
    private String stackTrace;
    private String className;
    private String methodName;
}
