package br.com.vibetex.domain.models.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.vibetex.domain.models.enums.TicketMessageAuthorType;

public record TicketMessageResponseDTO(
        UUID id,
        UUID ticketId,
        UUID autorUsuarioId,
        TicketMessageAuthorType autorTipo,
        String texto,
        OffsetDateTime criadoEm
) {}
