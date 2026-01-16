package br.com.vibetex.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vibetex.domain.models.entities.TicketFoto;

public interface TicketFotoRepository extends JpaRepository<TicketFoto, UUID> {
    List<TicketFoto> findByTicketIdOrderByCriadoEmDesc(UUID ticketId);
}
