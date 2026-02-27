package br.com.fightConnect.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_reads")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@IdClass(TicketReadId.class)
public class TicketRead {

    @Id
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "last_read_at", nullable = false)
    private OffsetDateTime lastReadAt;

    @PrePersist
    void prePersist() {
        if (lastReadAt == null) lastReadAt = OffsetDateTime.now();
    }
}
