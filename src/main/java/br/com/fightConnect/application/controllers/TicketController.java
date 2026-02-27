package br.com.fightConnect.application.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.fightConnect.domain.contracts.services.TicketService;
import br.com.fightConnect.domain.models.dtos.CreateTicketMessageRequestDTO;
import br.com.fightConnect.domain.models.dtos.CreateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.FinalizarTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.MarkTicketReadRequestDTO;
import br.com.fightConnect.domain.models.dtos.TicketMessageResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketResponseDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketStatusRequestDTO;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Tickets")
public class TicketController {

    private final TicketService ticketService;

    // ==========================
    // TICKETS
    // ==========================
    @Operation(summary = "Criar ticket")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponseDTO criar(@RequestBody @Valid CreateTicketRequestDTO dto) {
        return ticketService.criar(dto);
    }

    @Operation(summary = "Buscar ticket por id")
    @GetMapping("/{id}")
    public TicketResponseDTO buscarPorId(@PathVariable @NotNull UUID id) {
        return ticketService.buscarPorId(id);
    }

    @Operation(summary = "Listar tickets (filtros opcionais: usuarioId, equipeId, periodo)")
    @GetMapping
    public Page<TicketResponseDTO> listar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID equipeId,

            // ✅ NOVO (para calcular hasUnread no backend - Opção 4A)
            @RequestParam(required = false) UUID viewerId,

            @RequestParam(defaultValue = "false") boolean abertos,
            @RequestParam(required = false) List<TicketStatus> status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE_TIME)
            OffsetDateTime dataInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE_TIME)
            OffsetDateTime dataFim,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "criadoEm") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ticketService.listarFiltrado(
                usuarioId,
                equipeId,
                viewerId,
                abertos,
                status,
                dataInicio,
                dataFim,
                pageable
        );
    }

    @Operation(summary = "Atualizar ticket")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TicketResponseDTO atualizar(
            @PathVariable @NotNull UUID id,
            @RequestBody @Valid UpdateTicketRequestDTO dto
    ) {
        return ticketService.atualizar(id, dto);
    }

    @Operation(summary = "Atualizar status (NÃO finaliza; para RESOLVIDO/FECHADO use /finalizar)")
    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TicketResponseDTO atualizarStatus(
            @PathVariable @NotNull UUID id,
            @RequestBody @Valid UpdateTicketStatusRequestDTO dto
    ) {
        return ticketService.atualizarStatus(id, dto.status());
    }

    @Operation(summary = "Deletar ticket")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable @NotNull UUID id) {
        ticketService.deletar(id);
    }

    // ==========================
    // MENSAGENS (ticket_messages)
    // ==========================
    @Operation(summary = "Listar mensagens do ticket")
    @GetMapping("/{ticketId}/mensagens")
    public List<TicketMessageResponseDTO> listarMensagens(@PathVariable @NotNull UUID ticketId) {
        return ticketService.listar(ticketId);
    }

    @Operation(summary = "Adicionar mensagem ao ticket")
    @PostMapping(value = "/{ticketId}/mensagens", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TicketMessageResponseDTO adicionarMensagem(
            @PathVariable @NotNull UUID ticketId,
            @RequestParam @NotNull UUID autorUsuarioId,
            @RequestBody @Valid CreateTicketMessageRequestDTO dto
    ) {
        return ticketService.adicionar(ticketId, autorUsuarioId, dto);
    }

    @Operation(summary = "Responder e finalizar (cria mensagem + muda status para RESOLVIDO/FECHADO)")
    @PostMapping(value = "/{ticketId}/finalizar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void responderEFinalizar(
            @PathVariable @NotNull UUID ticketId,
            @RequestParam @NotNull UUID autorUsuarioId,
            @RequestBody @Valid FinalizarTicketRequestDTO dto
    ) {
        ticketService.responderEFinalizar(ticketId, autorUsuarioId, dto);
    }

    // ==========================
    // READ / VISUALIZADO
    // ==========================
    @Operation(summary = "Marcar ticket como lido (visualizado)")
    @PostMapping(value = "/{ticketId}/read", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarComoLido(
            @PathVariable @NotNull UUID ticketId,
            @RequestBody @Valid MarkTicketReadRequestDTO dto
    ) {
        ticketService.marcarComoLido(ticketId, dto.usuarioId());
    }
}
