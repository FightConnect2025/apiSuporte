package br.com.vibetex.domain.models.dtos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.vibetex.domain.models.enums.TicketStatus;

public record TicketResponseDTO(
		   UUID id,
	        UUID usuarioId,
	        UUID vistoriaId,
	        String titulo,
	        String descricao,
	        String numeroVistoria,

	        // ✅ NOVOS
	        Long numeroTicket,
	        Integer reaberturaSeq,
	        String numeroExibicao,

	        TicketStatus status,
	        OffsetDateTime criadoEm,
	        OffsetDateTime atualizadoEm,
	        OffsetDateTime fechadoEm,
	        String nomeUsuario,
	        String nomeEmpresa,
	        List<TicketFotoResponseDTO> fotos
) {}
