package br.com.vibetex.domain.models.dtos;

import br.com.vibetex.domain.models.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequestDTO(
        @NotNull TicketStatus status
) {}
