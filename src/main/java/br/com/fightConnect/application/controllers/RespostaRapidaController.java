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

import br.com.fightConnect.domain.models.entities.RespostaRapida;
import br.com.fightConnect.infrastructure.repositories.RespostaRapidaRepository;
import br.com.fightConnect.infrastructure.utils.HtmlSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/suporte/respostas-rapidas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
@Tag(name = "Respostas Rapidas")
public class RespostaRapidaController {

    private final RespostaRapidaRepository repository;
    private final HtmlSanitizer htmlSanitizer;

    @Operation(summary = "Listar respostas rapidas de uma equipe")
    @GetMapping
    public List<RespostaRapida> listar(@RequestParam UUID equipeId) {
        return repository.findByEquipeIdOrderByTituloAsc(equipeId);
    }

    @Operation(summary = "Criar resposta rapida")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaRapida criar(@RequestBody @Valid CriarRespostaRapidaRequest dto) {
        RespostaRapida rr = RespostaRapida.builder()
                .equipeId(dto.equipeId())
                .titulo(htmlSanitizer.sanitize(dto.titulo()))
                .conteudo(htmlSanitizer.sanitizeKeepNewlines(dto.conteudo()))
                .atalho(dto.atalho())
                .build();
        return repository.save(rr);
    }

    @Operation(summary = "Atualizar resposta rapida")
    @PutMapping("/{id}")
    public RespostaRapida atualizar(@PathVariable UUID id, @RequestBody @Valid CriarRespostaRapidaRequest dto) {
        RespostaRapida rr = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resposta rapida nao encontrada"));
        rr.setTitulo(htmlSanitizer.sanitize(dto.titulo()));
        rr.setConteudo(htmlSanitizer.sanitizeKeepNewlines(dto.conteudo()));
        rr.setAtalho(dto.atalho());
        return repository.save(rr);
    }

    @Operation(summary = "Deletar resposta rapida")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        repository.deleteById(id);
    }

    public record CriarRespostaRapidaRequest(
            @NotBlank UUID equipeId,
            @NotBlank @Size(max = 200) String titulo,
            @NotBlank String conteudo,
            String atalho
    ) {}
}
