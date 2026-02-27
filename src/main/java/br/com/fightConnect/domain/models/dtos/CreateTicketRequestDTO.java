package br.com.fightConnect.domain.models.dtos;

import java.util.List;
import java.util.UUID;

public record CreateTicketRequestDTO(
	    UUID usuarioId,
	    UUID equipeId,
	    UUID planoId,
	    String nomePlano,
	    String nomeEquipe,
	    String titulo,
	    String descricao,
	    List<String> fotosBase64 // ✅ novo, opcional
	) {}