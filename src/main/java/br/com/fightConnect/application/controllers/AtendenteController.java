package br.com.fightConnect.application.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.fightConnect.domain.contracts.services.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Atendente")
public class AtendenteController {

    private final TicketService ticketService;

    @Operation(summary = "Atribuir ticket a um atendente")
    @PostMapping("/{ticketId}/atribuir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atribuir(
            @PathVariable @NotNull UUID ticketId,
            @RequestBody @Valid AtribuirRequest dto
    ) {
        ticketService.atribuirAtendente(ticketId, dto.atendenteId(), dto.atendenteNome());
    }

    public record AtribuirRequest(
            @NotNull UUID atendenteId,
            @NotBlank String atendenteNome
    ) {}
}
