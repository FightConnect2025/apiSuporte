package br.com.fightConnect.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_slas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TicketSla {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "equipe_id")
    private UUID equipeId;

    @Column(name = "plano_id")
    private UUID planoId;

    /** Tempo maximo para primeira resposta (em minutos) */
    @Column(name = "tempo_primeira_resposta_min", nullable = false)
    @Builder.Default
    private Integer tempoPrimeiraRespostaMin = 60;

    /** Tempo maximo para resolucao (em minutos) */
    @Column(name = "tempo_resolucao_min", nullable = false)
    @Builder.Default
    private Integer tempoResolucaoMin = 240;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (criadoEm == null) criadoEm = now;
        if (atualizadoEm == null) atualizadoEm = now;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }
}
