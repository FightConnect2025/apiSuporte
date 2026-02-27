package br.com.vibetex.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import br.com.vibetex.domain.models.enums.TicketMessageAuthorType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_messages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TicketMessage {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "autor_usuario_id", nullable = false)
    private UUID autorUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "autor_tipo", nullable = false, length = 20)
    private TicketMessageAuthorType autorTipo;

    @Column(name = "texto", nullable = false, columnDefinition = "text")
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
    }
}
