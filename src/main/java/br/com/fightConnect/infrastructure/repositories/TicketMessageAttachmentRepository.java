package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.TicketMessageAttachment;

public interface TicketMessageAttachmentRepository extends JpaRepository<TicketMessageAttachment, UUID> {

    List<TicketMessageAttachment> findByMessageId(UUID messageId);
}
