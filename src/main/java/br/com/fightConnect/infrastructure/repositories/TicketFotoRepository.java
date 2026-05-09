package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.fightConnect.domain.models.entities.TicketFoto;

public interface TicketFotoRepository extends JpaRepository<TicketFoto, UUID> {

    List<TicketFoto> findByTicketIdOrderByCriadoEmDesc(UUID ticketId);

    @Query("SELECT f FROM TicketFoto f WHERE f.ticketId IN :ids ORDER BY f.criadoEm DESC")
    List<TicketFoto> findByTicketIdIn(@Param("ids") List<UUID> ids);

    default Map<UUID, List<TicketFoto>> findMapByTicketIdIn(List<UUID> ids) {
        return findByTicketIdIn(ids).stream()
                .collect(Collectors.groupingBy(TicketFoto::getTicketId));
    }
}
