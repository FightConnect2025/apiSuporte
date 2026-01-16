package br.com.vibetex.domain.models.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketFotoResponseDTO(
        UUID id,
        String fileName,
        String contentType,
        long sizeBytes,
        String url,
        OffsetDateTime criadoEm
) {}
