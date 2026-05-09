package br.com.fightConnect.domain.models.dtos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.com.fightConnect.domain.models.enums.TicketCategoria;
import br.com.fightConnect.domain.models.enums.TicketPrioridade;
import br.com.fightConnect.domain.models.enums.TicketStatus;

public record TicketResponseDTO(
        UUID id,
        UUID usuarioId,
        UUID planoId,
        UUID equipeId,
        String titulo,
        String descricao,
        Long numeroTicket,
        Integer reaberturaSeq,
        String numeroExibicao,
        TicketStatus status,
        TicketCategoria categoria,
        TicketPrioridade prioridade,
        UUID atendenteId,
        String atendenteNome,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm,
        OffsetDateTime fechadoEm,
        OffsetDateTime primeiraRespostaEm,
        String nomeUsuario,
        String nomeEquipe,
        String nomePlano,
        List<TicketFotoResponseDTO> fotos
) {}
