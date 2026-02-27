package br.com.fightConnect.domain.models.dtos;

import java.util.List;
import java.util.UUID;

public record UpdateTicketRequestDTO(
        UUID equipeId,
	    String titulo,
	    UUID planoId,
	    String nomePlano,
	    String nomeEquipe,
	    String descricao,
	    List<String> fotosBase64 // ✅ novo, opcional
) {}
