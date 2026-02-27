package br.com.fightConnect.infrastructure.repositories.specs;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.com.fightConnect.domain.models.entities.Ticket;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import jakarta.persistence.criteria.Predicate;

public final class TicketSpecifications {

    private TicketSpecifications() {}

    public static Specification<Ticket> filtro(
            UUID usuarioId,
            UUID equipeId,
            List<TicketStatus> status,
            OffsetDateTime inicio,
            OffsetDateTime fim
    ) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (usuarioId != null) {
                p = cb.and(p, cb.equal(root.get("usuarioId"), usuarioId));
            }

            if (equipeId != null) {
                p = cb.and(p, cb.equal(root.get("equipeId"), equipeId));
            }

            if (status != null && !status.isEmpty()) {
                p = cb.and(p, root.get("status").in(status));
            }

            if (inicio != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio));
            }

            if (fim != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("criadoEm"), fim));
            }

            return p;
        };
    }
}
