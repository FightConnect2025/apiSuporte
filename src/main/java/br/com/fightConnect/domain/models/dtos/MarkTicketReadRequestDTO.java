package br.com.fightConnect.domain.models.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record MarkTicketReadRequestDTO(@NotNull UUID usuarioId) {}
