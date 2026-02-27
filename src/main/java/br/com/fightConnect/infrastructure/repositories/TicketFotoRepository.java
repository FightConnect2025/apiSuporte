package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.TicketFoto;

public interface TicketFotoRepository extends JpaRepository<TicketFoto, UUID> {
    List<TicketFoto> findByTicketIdOrderByCriadoEmDesc(UUID ticketId);
}
