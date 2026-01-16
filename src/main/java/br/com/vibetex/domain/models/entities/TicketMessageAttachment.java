package br.com.vibetex.domain.models.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_message_attachments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TicketMessageAttachment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private TicketMessage message;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "content_type", nullable = true, length = 100)
    private String contentType;

    @Column(name = "file_name", nullable = true, length = 255)
    private String fileName;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
    }
}
