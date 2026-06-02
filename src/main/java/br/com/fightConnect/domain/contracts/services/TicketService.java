package br.com.fightConnect.domain.contracts.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.fightConnect.domain.models.dtos.CreateTicketMessageRequestDTO;
import br.com.fightConnect.domain.models.dtos.CreateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.FinalizarTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.TicketFotoResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketMessageResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketResponseDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketStatusRequestDTO;
import br.com.fightConnect.domain.models.enums.TicketStatus;

public interface TicketService {
    TicketResponseDTO criar(CreateTicketRequestDTO dto, UUID usuarioIdJwt);
    TicketResponseDTO buscarPorId(UUID id);
    Page<TicketResponseDTO> listarFiltrado(UUID usuarioId, UUID equipeId, UUID viewerId, boolean apenasAbertos,
            List<TicketStatus> status, OffsetDateTime dataInicio, OffsetDateTime dataFim, Pageable pageable);
    List<TicketMessageResponseDTO> listar(UUID ticketId);
    TicketMessageResponseDTO adicionar(UUID ticketId, UUID autorUsuarioId, CreateTicketMessageRequestDTO dto);
    TicketMessageResponseDTO adicionar(UUID ticketId, UUID autorUsuarioId, CreateTicketMessageRequestDTO dto,
            List<org.springframework.web.multipart.MultipartFile> anexos);
    void responderEFinalizar(UUID ticketId, UUID autorUsuarioId, FinalizarTicketRequestDTO dto);
    TicketResponseDTO atualizar(UUID id, UpdateTicketRequestDTO dto);
    void marcarComoLido(UUID ticketId, UUID usuarioId);
    TicketResponseDTO atualizarStatus(UUID id, TicketStatus status);
    void deletar(UUID id);
    TicketFotoResponseDTO buscarFoto(UUID ticketId, UUID imagemId);
    String exportarCsv(UUID usuarioId, UUID equipeId, List<TicketStatus> status,
            OffsetDateTime dataInicio, OffsetDateTime dataFim);
    void atribuirAtendente(UUID ticketId, UUID atendenteId, String atendenteNome);
    void salvarFeedback(UUID ticketId, int nota, String comentario);
}
