package br.com.fightConnect.domain.models.dtos;

import br.com.fightConnect.domain.models.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequestDTO(
        @NotNull TicketStatus status
) {}
