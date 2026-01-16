package br.com.vibetex.domain.mappers;

import br.com.vibetex.domain.models.dtos.TicketMessageResponseDTO;
import br.com.vibetex.domain.models.entities.TicketMessage;

public class TicketMessageMapper {

    private TicketMessageMapper() {}

    public static TicketMessageResponseDTO toDTO(TicketMessage m) {
        return new TicketMessageResponseDTO(
                m.getId(),
                m.getTicket().getId(),
                m.getAutorUsuarioId(),
                m.getAutorTipo(),
                m.getTexto(),
                m.getCriadoEm()
        );
    }
}
