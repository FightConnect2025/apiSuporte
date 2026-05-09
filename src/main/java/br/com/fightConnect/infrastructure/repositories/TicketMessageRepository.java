package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.TicketMessage;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    Page<TicketMessage> findByTicket_IdOrderByCriadoEmAsc(UUID ticketId, Pageable pageable);
    List<TicketMessage> findByTicket_IdOrderByCriadoEmAsc(UUID ticketId);

}
