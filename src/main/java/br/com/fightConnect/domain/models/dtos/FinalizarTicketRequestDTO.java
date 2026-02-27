package br.com.fightConnect.domain.models.dtos;

import br.com.fightConnect.domain.models.enums.TicketMessageAuthorType;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinalizarTicketRequestDTO(
        @NotNull TicketMessageAuthorType autorTipo,
        @NotBlank String textoResposta,
        @NotNull TicketStatus statusFinal
) {}
