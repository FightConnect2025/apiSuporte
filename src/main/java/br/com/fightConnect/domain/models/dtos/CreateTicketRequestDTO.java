package br.com.fightConnect.domain.models.dtos;

import java.util.List;
import java.util.UUID;

import br.com.fightConnect.domain.models.enums.TicketCategoria;
import br.com.fightConnect.domain.models.enums.TicketPrioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequestDTO(
        @NotNull UUID usuarioId,
        @NotNull UUID equipeId,
        UUID planoId,
        String nomePlano,
        @NotBlank String nomeEquipe,
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 10000) String descricao,
        @Size(max = 5) List<@NotBlank String> fotosBase64,
        TicketCategoria categoria,
        TicketPrioridade prioridade
) {}
