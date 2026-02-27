package br.com.vibetex.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.vibetex.domain.models.dtos.TicketResponseDTO;
import br.com.vibetex.domain.models.entities.Ticket;
import br.com.vibetex.domain.models.enums.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByUsuarioId(UUID usuarioId, Pageable pageable);

    Page<Ticket> findByUsuarioIdAndStatusIn(UUID usuarioId, List<TicketStatus> status, Pageable pageable);
    List<Ticket> findByUsuarioIdAndStatusIn(UUID usuarioId, List<TicketStatus> status);
    Page<Ticket> findByStatusIn(List<TicketStatus> status, Pageable pageable);

    @Query(value = "select nextval('ticket_num_seq')", nativeQuery = true)
    Long nextNumeroTicket();
}
