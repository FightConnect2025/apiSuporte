package br.com.vibetex.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import br.com.vibetex.domain.models.enums.TicketStatus;
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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.var;

@Entity
@Table(
    name = "tickets",
    indexes = {
        @Index(name = "ix_ticket_numero", columnList = "numero_ticket"),
        @Index(name = "ix_ticket_usuario", columnList = "usuario_id"),
        @Index(name = "ix_ticket_status", columnList = "status")
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

    @Column(name = "vistoria_id")
    private UUID vistoriaId;

    @Column(name = "numero_vistoria", nullable = false)
    private String numeroVistoria;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TicketStatus status;

    @Column(name = "nome_usuario", nullable = false, length = 200)
    private String nomeUsuario;

    @Column(name = "nome_empresa", nullable = false, length = 200)
    private String nomeEmpresa;
    
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC")
    private java.util.List<TicketMessage> mensagens = new java.util.ArrayList<>();


    // ==========================
    // ✅ NÚMERO DO TICKET (EXIBIÇÃO)
    // ==========================

    /**
     * Sequencial do ticket (ex: 1, 2, 3...). No front você formata como 0001.
     * Recomendado ser gerado via sequence ou tabela contador.
     */
    @Column(name = "numero_ticket", nullable = false)
    private Long numeroTicket;

    /**
     * Reabertura do ticket.
     * 0 = ticket original -> "0001"
     * 1 = primeira reabertura -> "0001-1"
     * 2 = segunda reabertura -> "0001-2"
     */
    @Column(name = "reabertura_seq", nullable = false)
    @Builder.Default
    private Integer reaberturaSeq = 0;

    /**
     * Número exibido (não salva no banco).
     */
    @Transient
    public String getNumeroExibicao() {
        String base = String.format("%04d", numeroTicket == null ? 0 : numeroTicket);
        if (reaberturaSeq == null || reaberturaSeq <= 0) return base;
        return base + "-" + reaberturaSeq;
    }

    // ==========================
    // Datas
    // ==========================
    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "fechado_em")
    private OffsetDateTime fechadoEm;

    @PrePersist
    public void prePersist() {
        var now = OffsetDateTime.now();
        this.criadoEm = (this.criadoEm == null) ? now : this.criadoEm;
        this.atualizadoEm = (this.atualizadoEm == null) ? now : this.atualizadoEm;
        if (this.reaberturaSeq == null) this.reaberturaSeq = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
        if (this.reaberturaSeq == null) this.reaberturaSeq = 0;
    }
}
