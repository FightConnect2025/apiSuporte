package br.com.vibetex.domain.models.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketMessageRequestDTO(
        @NotBlank String texto
) {}
