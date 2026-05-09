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

import br.com.fightConnect.domain.models.entities.Artigo;
import br.com.fightConnect.infrastructure.repositories.ArtigoRepository;
import br.com.fightConnect.infrastructure.utils.HtmlSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping(value = "/api/suporte/artigos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Artigos / FAQ")
public class ArtigoController {

    private final ArtigoRepository artigoRepository;
    private final HtmlSanitizer htmlSanitizer;

    @Operation(summary = "Listar artigos publicos")
    @GetMapping
    public List<Artigo> listar(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            return artigoRepository.findByTituloContainingIgnoreCaseOrTagsContainingIgnoreCase(q, q);
        }
        return artigoRepository.findByPublicadoTrueOrderByOrdemAsc();
    }

    @Operation(summary = "Buscar artigo por id")
    @GetMapping("/{id}")
    public Artigo buscarPorId(@PathVariable UUID id) {
        return artigoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artigo nao encontrado"));
    }

    @Operation(summary = "Criar artigo (requer ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Artigo criar(@RequestBody @Valid CriarArtigoRequest dto) {
        Artigo artigo = Artigo.builder()
                .equipeId(dto.equipeId())
                .titulo(htmlSanitizer.sanitize(dto.titulo()))
                .conteudo(htmlSanitizer.sanitizeKeepNewlines(dto.conteudo()))
                .tags(dto.tags())
                .categoria(dto.categoria())
                .publicado(dto.publicado())
                .ordem(dto.ordem())
                .build();
        return artigoRepository.save(artigo);
    }

    @Operation(summary = "Atualizar artigo (requer ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Artigo atualizar(@PathVariable UUID id, @RequestBody @Valid CriarArtigoRequest dto) {
        Artigo artigo = artigoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artigo nao encontrado"));
        artigo.setTitulo(htmlSanitizer.sanitize(dto.titulo()));
        artigo.setConteudo(htmlSanitizer.sanitizeKeepNewlines(dto.conteudo()));
        artigo.setTags(dto.tags());
        artigo.setCategoria(dto.categoria());
        artigo.setPublicado(dto.publicado());
        artigo.setOrdem(dto.ordem());
        if (dto.equipeId() != null) artigo.setEquipeId(dto.equipeId());
        return artigoRepository.save(artigo);
    }

    @Operation(summary = "Deletar artigo (requer ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void deletar(@PathVariable UUID id) {
        artigoRepository.deleteById(id);
    }

    public record CriarArtigoRequest(
            UUID equipeId,
            @NotBlank @Size(max = 300) String titulo,
            @NotBlank String conteudo,
            String tags,
            String categoria,
            boolean publicado,
            int ordem
    ) {}
}
