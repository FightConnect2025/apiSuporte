package br.com.fightConnect.infrastructure.streaming;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class LogAuditoriaEvent {
    private UUID usuarioId;
    private UUID equipeId;
    private String tipo;
    private String entidade;
    private String descricao;
    private String metodoHttp;
    private String url;
    private String ip;
    private String userAgent;
    private Long duracaoMs;
    private Map<String, Object> dados;
}
