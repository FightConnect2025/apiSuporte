package br.com.fightConnect.domain.models.dtos;

import br.com.fightConnect.domain.models.enums.TicketMessageAuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketMessageRequestDTO(
        @NotNull TicketMessageAuthorType autorTipo,
        @NotBlank @Size(max = 5000) String texto
) {}
