package br.com.fightConnect.application.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.fightConnect.domain.components.TicketFotoPorEquipeComponent;
import br.com.fightConnect.domain.models.dtos.TicketFotoResponseDTO;
import br.com.fightConnect.domain.models.entities.Ticket;
import br.com.fightConnect.infrastructure.repositories.TicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Upload")
public class UploadController {

    private final TicketRepository ticketRepository;
    private final TicketFotoPorEquipeComponent ticketFotoPorEquipeComponent;

    @Operation(summary = "Upload multipart de fotos para um ticket")
    @PostMapping(value = "/{ticketId}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<TicketFotoResponseDTO> uploadFotos(
            @PathVariable UUID ticketId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket nao encontrado"));

        List<String> fotosBase64 = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null) contentType = "image/png";
            String base64 = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            fotosBase64.add(base64);
        }

        ticketFotoPorEquipeComponent.salvar(
                ticket.getId(), ticket.getEquipeId(), ticket.getNomeEquipe(), fotosBase64
        );

        return List.of();
    }
}
