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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import br.com.fightConnect.domain.models.dtos.TicketFotoResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketMessageResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketResponseDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketStatusRequestDTO;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import br.com.fightConnect.infrastructure.security.JwtClaimsAdapter;
import br.com.fightConnect.infrastructure.storage.LocalFileStorageService;
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
    private final LocalFileStorageService storageService;

    @Operation(summary = "Criar ticket")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponseDTO criar(
            @RequestBody @Valid CreateTicketRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ticketService.criar(dto, JwtClaimsAdapter.usuarioId(jwt));
    }

    @Operation(summary = "Buscar ticket por id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TicketResponseDTO buscarPorId(@PathVariable @NotNull UUID id) {
        return ticketService.buscarPorId(id);
    }

    @Operation(summary = "Listar tickets (filtros opcionais: equipeId, periodo)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<TicketResponseDTO> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID equipeId,
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
        UUID usuarioId = JwtClaimsAdapter.usuarioId(jwt);
        return ticketService.listarFiltrado(
                usuarioId, equipeId, usuarioId,
                abertos, status, dataInicio, dataFim, pageable
        );
    }

    @Operation(summary = "Atualizar ticket")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_GESTOR')")
    public TicketResponseDTO atualizar(
            @PathVariable @NotNull UUID id,
            @RequestBody @Valid UpdateTicketRequestDTO dto
    ) {
        return ticketService.atualizar(id, dto);
    }

    @Operation(summary = "Atualizar status (NAO finaliza; para RESOLVIDO/FECHADO use /finalizar)")
    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_GESTOR')")
    public TicketResponseDTO atualizarStatus(
            @PathVariable @NotNull UUID id,
            @RequestBody @Valid UpdateTicketStatusRequestDTO dto
    ) {
        return ticketService.atualizarStatus(id, dto.status());
    }

    @Operation(summary = "Deletar ticket")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_GESTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable @NotNull UUID id) {
        ticketService.deletar(id);
    }

    @Operation(summary = "Listar mensagens do ticket")
    @GetMapping("/{ticketId}/mensagens")
    @PreAuthorize("isAuthenticated()")
    public List<TicketMessageResponseDTO> listarMensagens(@PathVariable @NotNull UUID ticketId) {
        return ticketService.listar(ticketId);
    }

    @Operation(summary = "Adicionar mensagem ao ticket (autorId extraido do JWT)")
    @PostMapping(value = "/{ticketId}/mensagens", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketMessageResponseDTO adicionarMensagem(
            @PathVariable @NotNull UUID ticketId,
            @RequestBody @Valid CreateTicketMessageRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ticketService.adicionar(ticketId, JwtClaimsAdapter.usuarioId(jwt), dto);
    }

    @Operation(summary = "Adicionar mensagem ao ticket com anexos (autorId extraido do JWT)")
    @PostMapping(value = "/{ticketId}/mensagens/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketMessageResponseDTO adicionarMensagemComAnexos(
            @PathVariable @NotNull UUID ticketId,
            @RequestParam("autorTipo") br.com.fightConnect.domain.models.enums.TicketMessageAuthorType autorTipo,
            @RequestParam("texto") String texto,
            @RequestParam(value = "files", required = false) List<org.springframework.web.multipart.MultipartFile> files,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var dto = new CreateTicketMessageRequestDTO(autorTipo, texto);
        return ticketService.adicionar(ticketId, JwtClaimsAdapter.usuarioId(jwt), dto, files);
    }

    @Operation(summary = "Responder e finalizar (autorId extraido do JWT)")
    @PostMapping(value = "/{ticketId}/finalizar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void responderEFinalizar(
            @PathVariable @NotNull UUID ticketId,
            @RequestBody @Valid FinalizarTicketRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ticketService.responderEFinalizar(ticketId, JwtClaimsAdapter.usuarioId(jwt), dto);
    }

    @Operation(summary = "Marcar ticket como lido (usuarioId extraido do JWT)")
    @PostMapping(value = "/{ticketId}/read")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarComoLido(
            @PathVariable @NotNull UUID ticketId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ticketService.marcarComoLido(ticketId, JwtClaimsAdapter.usuarioId(jwt));
    }

    @Operation(summary = "Servir imagem de ticket com verificacao de acesso")
    @GetMapping("/{ticketId}/imagens/{imagemId}")
    @PreAuthorize("isAuthenticated()")
    public TicketFotoResponseDTO buscarImagem(
            @PathVariable @NotNull UUID ticketId,
            @PathVariable @NotNull UUID imagemId
    ) {
        return ticketService.buscarFoto(ticketId, imagemId);
    }

    @Operation(summary = "Servir arquivo de imagem (streaming autenticado)")
    @GetMapping("/{ticketId}/imagens/{imagemId}/arquivo")
    @PreAuthorize("isAuthenticated()")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> servirImagem(
            @PathVariable @NotNull UUID ticketId,
            @PathVariable @NotNull UUID imagemId
    ) {
        var foto = ticketService.buscarFoto(ticketId, imagemId);
        var resource = storageService.carregarComoResource(foto.fileName(), ticketId.toString());
        if (resource == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String contentType = foto.contentType() != null ? foto.contentType() : "image/png";
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @Operation(summary = "Exportar tickets filtrados como CSV")
    @GetMapping("/exportar/csv")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPER_GESTOR')")
    public String exportarCsv(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID equipeId,
            @RequestParam(required = false) List<TicketStatus> status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE_TIME)
            OffsetDateTime dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE_TIME)
            OffsetDateTime dataFim
    ) {
        return ticketService.exportarCsv(
                JwtClaimsAdapter.usuarioId(jwt), equipeId, status, dataInicio, dataFim
        );
    }
}
