package br.com.fightConnect.domain.models.dtos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.fightConnect.domain.models.enums.TicketStatus;

public record TicketResponseDTO(
		   UUID id,
	        UUID usuarioId,
	        UUID planoId,
	        UUID equipeId,
	        String titulo,
	        String descricao,

	        // ✅ NOVOS
	        Long numeroTicket,
	        Integer reaberturaSeq,
	        String numeroExibicao,

	        TicketStatus status,
	        OffsetDateTime criadoEm,
	        OffsetDateTime atualizadoEm,
	        OffsetDateTime fechadoEm,
	        String nomeUsuario,
	        String nomeEquipe,
	        String nomePlano,
	        List<TicketFotoResponseDTO> fotos
) {}
