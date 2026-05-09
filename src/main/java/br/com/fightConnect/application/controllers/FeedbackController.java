package br.com.fightConnect.application.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Feedback")
public class FeedbackController {

    private final TicketService ticketService;

    @Operation(summary = "Avaliar ticket resolvido (CSAT)")
    @PostMapping("/{ticketId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public void avaliar(
            @PathVariable @NotNull UUID ticketId,
            @RequestBody @Valid FeedbackRequest dto
    ) {
        ticketService.salvarFeedback(ticketId, dto.nota(), dto.comentario());
    }

    public record FeedbackRequest(
            @Min(1) @Max(5) int nota,
            String comentario
    ) {}
}
