package br.com.fightConnect.domain.contracts.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.fightConnect.domain.models.dtos.CreateTicketMessageRequestDTO;
import br.com.fightConnect.domain.models.dtos.CreateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.FinalizarTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.TicketMessageResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketResponseDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.fightConnect.domain.models.enums.TicketStatus;

public interface TicketService {

	// ✅ JSON normal
	TicketResponseDTO criar(CreateTicketRequestDTO dto);

	Page<TicketResponseDTO> listarFiltrado(UUID usuarioId, UUID equipeId, boolean apenasAbertos,
			List<TicketStatus> status, OffsetDateTime dataInicio, OffsetDateTime dataFim, Pageable pageable);

	List<TicketMessageResponseDTO> listar(UUID ticketId);

	TicketMessageResponseDTO adicionar(UUID ticketId, UUID autorUsuarioId, CreateTicketMessageRequestDTO dto);

	void responderEFinalizar(UUID ticketId, UUID autorUsuarioId, FinalizarTicketRequestDTO dto);

	TicketResponseDTO atualizar(UUID id, UpdateTicketRequestDTO dto);

	void marcarComoLido(UUID ticketId, UUID usuarioId);

	TicketResponseDTO atualizarStatus(UUID id, TicketStatus status);

	void deletar(UUID id);

	TicketResponseDTO buscarPorId(UUID id);

	Page<TicketResponseDTO> listarTodos(boolean apenasAbertos, List<TicketStatus> status, Pageable pageable);

	Page<TicketResponseDTO> listarFiltrado(UUID usuarioId, UUID equipeId, UUID viewerId, boolean apenasAbertos,
			List<TicketStatus> status, OffsetDateTime dataInicio, OffsetDateTime dataFim, Pageable pageable);

	Page<TicketResponseDTO> listarPorUsuario(UUID usuarioId, boolean apenasAbertos, List<TicketStatus> status,
			Pageable pageable);
}
