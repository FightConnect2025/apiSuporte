package br.com.fightConnect.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.TicketFeedback;

public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, UUID> {
    Optional<TicketFeedback> findByTicketId(UUID ticketId);
    boolean existsByTicketId(UUID ticketId);
}
