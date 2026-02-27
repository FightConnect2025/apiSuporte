package br.com.vibetex.application.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import br.com.vibetex.domain.contracts.services.TicketService;
import br.com.vibetex.domain.models.dtos.CreateTicketRequestDTO;
import br.com.vibetex.domain.models.dtos.TicketResponseDTO;
import br.com.vibetex.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.vibetex.domain.models.dtos.UpdateTicketStatusRequestDTO;
import br.com.vibetex.domain.models.enums.TicketStatus;
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
    
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponseDTO criar(@RequestBody @Valid CreateTicketRequestDTO dto) {
        return ticketService.criar(dto);
    }


    @Operation(summary = "Atualizar status")
    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TicketResponseDTO atualizarStatus(
            @PathVariable @NotNull UUID id,
            @RequestBody @Valid UpdateTicketStatusRequestDTO dto
    ) {
        return ticketService.atualizarStatus(id, dto.status());
    }

    @Operation(summary = "Atualizar ticket")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TicketResponseDTO atualizar(
            @PathVariable @NotNull UUID id,
            @RequestBody @Valid UpdateTicketRequestDTO dto
    ) {
        return ticketService.atualizar(id, dto);
    }

    @Operation(summary = "Deletar ticket")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable @NotNull UUID id) {
        ticketService.deletar(id);
    }

    @Operation(summary = "Buscar por ID")
    @GetMapping("/{id}")
    public TicketResponseDTO buscarPorId(@PathVariable @NotNull UUID id) {
        return ticketService.buscarPorId(id);
    }

    @Operation(summary = "Listar tickets (por usuário opcional)")
    @GetMapping
    public Page<TicketResponseDTO> listar(
            @RequestParam(required = false) UUID usuarioId, // ✅ agora opcional
            @RequestParam(defaultValue = "false") boolean abertos,
            @RequestParam(required = false) List<TicketStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "criadoEm") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ se veio usuarioId -> filtra por usuário
        if (usuarioId != null) {
            return ticketService.listarPorUsuario(usuarioId, abertos, status, pageable);
        }

        // ✅ se não veio -> traz todos
        return ticketService.listarTodos(abertos, status, pageable);
    }

}
