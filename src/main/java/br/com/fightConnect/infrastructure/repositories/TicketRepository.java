package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import br.com.fightConnect.domain.models.entities.Ticket;
import br.com.fightConnect.domain.models.enums.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByUsuarioId(UUID usuarioId, Pageable pageable);
    Page<Ticket> findByEquipeId(UUID equipeId, Pageable pageable);

    Page<Ticket> findByUsuarioIdAndStatusIn(UUID usuarioId, List<TicketStatus> status, Pageable pageable);
    List<Ticket> findByUsuarioIdAndStatusIn(UUID usuarioId, List<TicketStatus> status);

    Page<Ticket> findByEquipeIdAndStatusIn(UUID equipeId, List<TicketStatus> status, Pageable pageable);
    Page<Ticket> findByStatusIn(List<TicketStatus> status, Pageable pageable);

    @Query(value = "SELECT nextval('ticket_num_seq')", nativeQuery = true)
    Long nextNumeroTicket();
}
