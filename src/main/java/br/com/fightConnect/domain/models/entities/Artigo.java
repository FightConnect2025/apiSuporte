package br.com.fightConnect.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artigos", indexes = {
    @Index(name = "ix_artigo_publicado", columnList = "publicado"),
    @Index(name = "ix_artigo_equipe", columnList = "equipe_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Artigo {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "equipe_id")
    private UUID equipeId;

    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Column(name = "conteudo", nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "publicado", nullable = false)
    @Builder.Default
    private boolean publicado = true;

    @Column(name = "ordem")
    @Builder.Default
    private int ordem = 0;

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
