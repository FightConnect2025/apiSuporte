package br.com.fightConnect.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.TicketRead;
import br.com.fightConnect.domain.models.entities.TicketReadId;

public interface TicketReadRepository extends JpaRepository<TicketRead, TicketReadId> {
    Optional<TicketRead> findByTicketIdAndUsuarioId(UUID ticketId, UUID usuarioId);
}
