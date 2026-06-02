package br.com.fightConnect.domain.models.dtos;

import java.util.UUID;

public record TicketMessageAttachmentResponseDTO(
        UUID id,
        String url,
        String contentType,
        String fileName
) {}
