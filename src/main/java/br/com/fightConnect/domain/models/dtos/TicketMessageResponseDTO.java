package br.com.fightConnect.domain.models.dtos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.fightConnect.domain.models.enums.TicketMessageAuthorType;

public record TicketMessageResponseDTO(UUID id, UUID ticketId, UUID autorUsuarioId, String autorNome,
		TicketMessageAuthorType autorTipo, String equipeNome, String texto, OffsetDateTime criadoEm,
		List<TicketMessageAttachmentResponseDTO> anexos) {
}
