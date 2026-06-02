package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.TicketSla;

public interface TicketSlaRepository extends JpaRepository<TicketSla, UUID> {

    Optional<TicketSla> findByEquipeIdAndPlanoIdAndAtivoTrue(UUID equipeId, UUID planoId);

    Optional<TicketSla> findByEquipeIdAndPlanoIdIsNullAndAtivoTrue(UUID equipeId);

    List<TicketSla> findByEquipeIdAndAtivoTrue(UUID equipeId);
}
