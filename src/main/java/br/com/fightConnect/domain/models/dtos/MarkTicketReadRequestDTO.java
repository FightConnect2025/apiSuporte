package br.com.fightConnect.domain.models.dtos;

import jakarta.validation.constraints.NotNull;

public record MarkTicketReadRequestDTO(@NotNull Long timestamp) {}
