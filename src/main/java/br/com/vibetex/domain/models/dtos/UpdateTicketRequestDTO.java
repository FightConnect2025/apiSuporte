package br.com.vibetex.domain.models.dtos;

import java.util.List;
import java.util.UUID;

public record UpdateTicketRequestDTO(
        UUID vistoriaId,
	    String titulo,
	    String numeroVistoria,
	    String descricao,
	    List<String> fotosBase64 // ✅ novo, opcional
) {}
