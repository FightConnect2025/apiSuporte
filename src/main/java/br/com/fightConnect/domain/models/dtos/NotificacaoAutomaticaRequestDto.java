package br.com.fightConnect.domain.models.dtos;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.fightConnect.domain.models.enums.TipoCanal;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificacaoAutomaticaRequestDto {

    @JsonProperty("equipeId")
    private UUID equipeId;

    /**
     * ✅ O worker procura "eventType" (camelCase).
     * Se seu ObjectMapper estiver em SNAKE_CASE, ele vira "event_type" e o worker NÃO ACHA.
     * Então aqui a gente força o nome do campo no JSON.
     */
    @JsonProperty("eventType")
    private String eventType;

    /**
     * Categoria (logs/organização)
     * Ex: "SUPORTE"
     */
    @JsonProperty("tipo")
    private String tipo;

    @JsonProperty("canal")
    private TipoCanal canal;

    /**
     * Pode ser PERFIL (ex: SUPER_ADMIN) ou userId (UUID string)
     */
    @JsonProperty("destinatario")
    private List<String> destinatario;

    // ✅ dados padrões
    @JsonProperty("titulo")
    private String titulo;

    @JsonProperty("corpo")
    private String corpo;

    @JsonProperty("url")
    private String url;

    @JsonProperty("tag")
    private String tag;

    // ✅ mantém para email/worker legado se você quiser
    @JsonProperty("assunto")
    private String assunto;

    @JsonProperty("mensagemHtml")
    private String mensagemHtml;

    @JsonProperty("mensagemResumo")
    private String mensagemResumo;

    // ✅ dados suporte (pra enriquecer no worker)
    @JsonProperty("ticketId")
    private String ticketId;

    @JsonProperty("ticketNumero")
    private String ticketNumero;

    @JsonProperty("ticketTitulo")
    private String ticketTitulo;

    @JsonProperty("usuarioNome")
    private String usuarioNome;

    @JsonProperty("status")
    private String status;
}
