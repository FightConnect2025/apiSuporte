package br.com.fightConnect.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import br.com.fightConnect.domain.models.enums.TicketCategoria;
import br.com.fightConnect.domain.models.enums.TicketPrioridade;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tickets",
    indexes = {
        @Index(name = "ix_ticket_numero", columnList = "numero_ticket"),
        @Index(name = "ix_ticket_usuario", columnList = "usuario_id"),
        @Index(name = "ix_ticket_status", columnList = "status"),
        @Index(name = "ix_ticket_equipe", columnList = "equipeId"),
        @Index(name = "ix_ticket_atendente", columnList = "atendente_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_numero", columnNames = {"numero_ticket"})
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "polano_id")
    private UUID planoId;

    @Column(name = "equipeId", nullable = false)
    private UUID equipeId;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private TicketCategoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", length = 10)
    private TicketPrioridade prioridade;

    @Column(name = "atendente_id")
    private UUID atendenteId;

    @Column(name = "atendente_nome", length = 200)
    private String atendenteNome;

    @Column(name = "nome_usuario", nullable = false, length = 200)
    private String nomeUsuario;

    @Column(name = "nome_equipe", nullable = false, length = 200)
    private String nomeEquipe;

    @Column(name = "nome_plano", nullable = false, length = 200)
    private String nomePlano;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC")
    @Builder.Default
    private java.util.List<TicketMessage> mensagens = new java.util.ArrayList<>();

    @Column(name = "numero_ticket", nullable = false)
    private Long numeroTicket;

    @Column(name = "reabertura_seq", nullable = false)
    @Builder.Default
    private Integer reaberturaSeq = 0;

    @Column(name = "numero_exibicao", length = 20)
    private String numeroExibicao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "fechado_em")
    private OffsetDateTime fechadoEm;

    @Column(name = "primeira_resposta_em")
    private OffsetDateTime primeiraRespostaEm;

    @PrePersist
    public void prePersist() {
        var now = OffsetDateTime.now();
        this.criadoEm = (this.criadoEm == null) ? now : this.criadoEm;
        this.atualizadoEm = (this.atualizadoEm == null) ? now : this.atualizadoEm;
        if (this.reaberturaSeq == null) this.reaberturaSeq = 0;
        if (this.numeroExibicao == null) {
            this.numeroExibicao = gerarNumeroExibicao();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
        if (this.reaberturaSeq == null) this.reaberturaSeq = 0;
        this.numeroExibicao = gerarNumeroExibicao();
    }

    public String gerarNumeroExibicao() {
        String base = String.format("%04d", numeroTicket == null ? 0 : numeroTicket);
        if (reaberturaSeq == null || reaberturaSeq <= 0) return base;
        return base + "-" + reaberturaSeq;
    }
}
