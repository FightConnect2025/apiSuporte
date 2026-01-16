package br.com.vibetex.domain.contracts.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.vibetex.domain.models.dtos.CreateTicketRequestDTO;
import br.com.vibetex.domain.models.dtos.TicketResponseDTO;
import br.com.vibetex.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.vibetex.domain.models.enums.TicketStatus;

public interface TicketService {

	// ✅ JSON normal
    TicketResponseDTO criar(CreateTicketRequestDTO dto);


    TicketResponseDTO atualizar(UUID id, UpdateTicketRequestDTO dto);

    TicketResponseDTO atualizarStatus(UUID id, TicketStatus status);

    void deletar(UUID id);

    TicketResponseDTO buscarPorId(UUID id);
    
    Page<TicketResponseDTO> listarTodos(boolean apenasAbertos, List<TicketStatus> status, Pageable pageable);


    Page<TicketResponseDTO> listarPorUsuario(UUID usuarioId, boolean apenasAbertos, List<TicketStatus> status, Pageable pageable);
}
