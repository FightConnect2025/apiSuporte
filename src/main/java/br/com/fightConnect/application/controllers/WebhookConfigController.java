package br.com.fightConnect.application.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.fightConnect.domain.models.entities.WebhookConfig;
import br.com.fightConnect.infrastructure.repositories.WebhookConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/suporte/webhooks", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Webhooks")
public class WebhookConfigController {

    private final WebhookConfigRepository repository;

    @Operation(summary = "Listar webhooks de uma equipe")
    @GetMapping
    public List<WebhookConfig> listar(@RequestParam @NotNull UUID equipeId) {
        return repository.findByEquipeIdAndAtivoTrue(equipeId);
    }

    @Operation(summary = "Buscar webhook por id")
    @GetMapping("/{id}")
    public WebhookConfig buscarPorId(@PathVariable @NotNull UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook nao encontrado"));
    }

    @Operation(summary = "Criar webhook")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookConfig criar(@RequestBody @Valid WebhookConfigRequest dto) {
        WebhookConfig config = WebhookConfig.builder()
                .equipeId(dto.equipeId())
                .url(dto.url())
                .eventos(dto.eventos())
                .segredo(dto.segredo())
                .ativo(dto.ativo())
                .build();
        return repository.save(config);
    }

    @Operation(summary = "Atualizar webhook")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WebhookConfig atualizar(@PathVariable @NotNull UUID id, @RequestBody @Valid WebhookConfigRequest dto) {
        WebhookConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook nao encontrado"));
        config.setEquipeId(dto.equipeId());
        config.setUrl(dto.url());
        config.setEventos(dto.eventos());
        config.setSegredo(dto.segredo());
        config.setAtivo(dto.ativo());
        return repository.save(config);
    }

    @Operation(summary = "Deletar webhook")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable @NotNull UUID id) {
        repository.deleteById(id);
    }

    public record WebhookConfigRequest(
            @NotNull UUID equipeId,
            @NotBlank @Size(max = 500) String url,
            @NotBlank @Size(max = 500) String eventos,
            @Size(max = 255) String segredo,
            boolean ativo
    ) {}
}
