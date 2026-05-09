package br.com.fightConnect.domain.models.dtos;

import java.util.List;
import java.util.UUID;

import br.com.fightConnect.domain.models.enums.TicketCategoria;
import br.com.fightConnect.domain.models.enums.TicketPrioridade;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequestDTO(
        UUID equipeId,
        @Size(max = 200) String titulo,
        UUID planoId,
        String nomePlano,
        String nomeEquipe,
        @Size(max = 10000) String descricao,
        List<String> fotosBase64,
        TicketCategoria categoria,
        TicketPrioridade prioridade
) {}
