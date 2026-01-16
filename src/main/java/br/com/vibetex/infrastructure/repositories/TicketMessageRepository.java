package br.com.vibetex.infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vibetex.domain.models.entities.TicketMessage;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    Page<TicketMessage> findByTicket_IdOrderByCriadoEmAsc(UUID ticketId, Pageable pageable);

}
