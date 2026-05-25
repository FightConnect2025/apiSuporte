package br.com.fightConnect.domain.services;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.fightConnect.domain.components.TicketFotoPorEquipeComponent;
import br.com.fightConnect.domain.contracts.services.FileStorageService;
import br.com.fightConnect.domain.contracts.services.TicketService;
import br.com.fightConnect.domain.models.dtos.CreateTicketMessageRequestDTO;
import br.com.fightConnect.domain.models.dtos.CreateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.FinalizarTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.TicketFotoResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketMessageResponseDTO;
import br.com.fightConnect.domain.models.dtos.TicketResponseDTO;
import br.com.fightConnect.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.fightConnect.domain.models.entities.Ticket;
import br.com.fightConnect.domain.models.entities.TicketFeedback;
import br.com.fightConnect.domain.models.entities.TicketFoto;
import br.com.fightConnect.domain.models.entities.TicketMessage;
import br.com.fightConnect.domain.models.entities.TicketRead;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAdminClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAlunoClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthProfessorClient;
import br.com.fightConnect.infrastructure.repositories.TicketFeedbackRepository;
import br.com.fightConnect.infrastructure.repositories.TicketFotoRepository;
import br.com.fightConnect.infrastructure.repositories.TicketMessageRepository;
import br.com.fightConnect.infrastructure.repositories.TicketReadRepository;
import br.com.fightConnect.infrastructure.repositories.TicketRepository;
import br.com.fightConnect.infrastructure.repositories.specs.TicketSpecifications;
import br.com.fightConnect.infrastructure.utils.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final ApiAuthAlunoClient apiAuthAlunoClient;
    private final ApiAuthProfessorClient apiAuthProfessorClient;
    private final ApiAuthAdminClient apiAuthAdminClient;

    private final TicketRepository repo;
    private final TicketFotoRepository fotoRepo;
    private final TicketMessageRepository messageRepo;
    private final TicketReadRepository readRepo;
    private final TicketFeedbackRepository feedbackRepo;

    private final TicketFotoPorEquipeComponent ticketFotoPorEquipeComponent;
    private final FileStorageService storage;
    private final HtmlSanitizer htmlSanitizer;
    private final TicketNotificationHelper notificationHelper;

    private static final List<TicketStatus> STATUS_ABERTOS = List
            .copyOf(EnumSet.of(TicketStatus.ABERTO, TicketStatus.EM_ANDAMENTO, TicketStatus.AGUARDANDO_USUARIO));

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public TicketResponseDTO criar(CreateTicketRequestDTO dto, UUID usuarioIdJwt) {
        validateCreate(dto);

        if (usuarioIdJwt == null)
            throw badRequest("usuarioId do JWT e obrigatorio");
        if (!dto.usuarioId().equals(usuarioIdJwt) && !isAdminDoToken()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "usuarioId nao corresponde ao usuario autenticado");
        }

        String nomeEquipe = defaultIfBlank(dto.nomeEquipe(), null);
        String nomePlano = defaultIfBlank(dto.nomePlano(), null);

        if (isBlank(nomeEquipe))
            throw badRequest("nomeEquipe e obrigatorio");
        if (isBlank(nomePlano))
            throw badRequest("nomePlano e obrigatorio");

        var infoUsuario = buscarInfoUsuario(dto.usuarioId());
        String nomeUsuario = defaultIfBlank(infoUsuario.nome(), "Usuario");

        Long numeroTicket = nextNumeroTicketOrThrow();

        Ticket ticket = Ticket.builder()
                .usuarioId(dto.usuarioId())
                .equipeId(dto.equipeId())
                .planoId(dto.planoId())
                .titulo(htmlSanitizer.sanitize(dto.titulo()))
                .descricao(htmlSanitizer.sanitizeKeepNewlines(dto.descricao()))
                .categoria(dto.categoria())
                .prioridade(dto.prioridade())
                .status(TicketStatus.ABERTO)
                .nomeUsuario(nomeUsuario)
                .nomeEquipe(nomeEquipe)
                .nomePlano(nomePlano)
                .numeroTicket(numeroTicket)
                .reaberturaSeq(0)
                .build();

        Ticket saved = repo.save(ticket);

        if (dto.fotosBase64() != null && !dto.fotosBase64().isEmpty()) {
            ticketFotoPorEquipeComponent.salvar(saved.getId(), saved.getEquipeId(), saved.getNomeEquipe(),
                    dto.fotosBase64());
        }

        notificationHelper.notifyCreated(saved);

        return toDTO(saved);
    }

    private void validateCreate(CreateTicketRequestDTO dto) {
        if (dto == null)
            throw badRequest("Payload invalido");
        if (dto.usuarioId() == null)
            throw badRequest("usuarioId e obrigatorio");
        if (dto.equipeId() == null)
            throw badRequest("equipeId e obrigatorio");
        if (isBlank(dto.titulo()))
            throw badRequest("titulo e obrigatorio");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "ticketsList", key = "#usuarioId + '-' + #equipeId + '-' + #apenasAbertos + '-' + #pageable.pageNumber")
    public Page<TicketResponseDTO> listarFiltrado(UUID usuarioId, UUID equipeId, UUID viewerId, boolean apenasAbertos,
            List<TicketStatus> status, OffsetDateTime dataInicio, OffsetDateTime dataFim, Pageable pageable) {

        UUID equipeIdToken = isAdminDoToken() ? null : getEquipeIdDoToken();
        UUID equipeParaFiltrar = equipeId;

        if (equipeIdToken != null) {
            if (equipeId != null && !equipeId.equals(equipeIdToken)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nao e permitido listar tickets de outra equipe.");
            }
            equipeParaFiltrar = equipeIdToken;
        }

        List<TicketStatus> filtroStatus = (status != null && !status.isEmpty()) ? status
                : (apenasAbertos ? STATUS_ABERTOS : null);

        Specification<Ticket> spec = TicketSpecifications.filtro(usuarioId, equipeParaFiltrar, filtroStatus, dataInicio, dataFim);

        Page<Ticket> page = repo.findAll(spec, pageable);

        batchLoadFotos(page.getContent());

        return page.map(this::toDTO);
    }

    private void batchLoadFotos(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) return;
        List<UUID> ids = tickets.stream().map(Ticket::getId).toList();
        Map<UUID, List<TicketFoto>> fotosMap = fotoRepo.findMapByTicketIdIn(ids);
        for (Ticket t : tickets) {
            List<TicketFoto> fotos = fotosMap.getOrDefault(t.getId(), List.of());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDTO buscarPorId(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        validarAcessoTicket(ticket);
        return toDTO(ticket);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public TicketResponseDTO atualizar(UUID id, UpdateTicketRequestDTO dto) {
        Ticket ticket = findTicketOrThrow(id);
        validarAcessoTicket(ticket);

        if (dto != null) {
            if (dto.equipeId() != null)
                ticket.setEquipeId(dto.equipeId());
            if (!isBlank(dto.nomeEquipe()))
                ticket.setNomeEquipe(dto.nomeEquipe());
            if (dto.planoId() != null)
                ticket.setPlanoId(dto.planoId());
            if (!isBlank(dto.nomePlano()))
                ticket.setNomePlano(dto.nomePlano());
            if (!isBlank(dto.titulo()))
                ticket.setTitulo(htmlSanitizer.sanitize(dto.titulo()));
            if (dto.descricao() != null)
                ticket.setDescricao(htmlSanitizer.sanitizeKeepNewlines(dto.descricao()));
        }

        refreshNomeUsuario(ticket);

        Ticket saved = repo.save(ticket);

        if (dto != null && dto.fotosBase64() != null && !dto.fotosBase64().isEmpty()) {
            ticketFotoPorEquipeComponent.salvar(saved.getId(), saved.getEquipeId(), saved.getNomeEquipe(),
                    dto.fotosBase64());
        }

        return toDTO(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public TicketResponseDTO atualizarStatus(UUID id, TicketStatus status) {
        if (status == null)
            throw badRequest("status e obrigatorio");

        if (isFinalStatus(status)) {
            throw badRequest("Para finalizar (RESOLVIDO/FECHADO), use o endpoint de responder e finalizar.");
        }

        Ticket ticket = findTicketOrThrow(id);
        validarAcessoTicket(ticket);

        TicketStatus old = ticket.getStatus();
        onReopenIfNeeded(ticket, old, status);
        ticket.setStatus(status);
        applyStatusLogic(ticket, status);

        Ticket saved = repo.save(ticket);

        if (old != status) {
            notificationHelper.notifyStatusChanged(saved, old, status);
        }

        return toDTO(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public void deletar(UUID id) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket nao encontrado"));

        validarAcessoTicket(ticket);

        List<TicketFoto> fotos = fotoRepo.findByTicketIdOrderByCriadoEmDesc(ticket.getId());
        for (TicketFoto f : fotos) {
            try {
                if (!isBlank(f.getStorageKey()))
                    storage.delete(f.getStorageKey());
            } catch (Exception ignored) {}
        }
        if (!fotos.isEmpty())
            fotoRepo.deleteAll(fotos);

        repo.delete(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketFotoResponseDTO buscarFoto(UUID ticketId, UUID imagemId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        validarAcessoTicket(ticket);

        TicketFoto foto = fotoRepo.findById(imagemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem nao encontrada"));

        if (!foto.getTicketId().equals(ticketId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem nao pertence a este ticket");
        }

        return toFotoDTO(foto);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportarCsv(UUID usuarioId, UUID equipeId, List<TicketStatus> status,
            OffsetDateTime dataInicio, OffsetDateTime dataFim) {
        UUID equipeIdToken = isAdminDoToken() ? null : getEquipeIdDoToken();
        UUID equipeParaFiltrar = equipeId != null ? equipeId : equipeIdToken;

        Specification<Ticket> spec = TicketSpecifications.filtro(usuarioId, equipeParaFiltrar, status, dataInicio, dataFim);
        List<Ticket> tickets = repo.findAll(spec);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,NUMERO,TITULO,STATUS,CATEGORIA,PRIORIDADE,USUARIO,EQUIPE,CRIADO EM,FECHADO EM,ATENDENTE\n");
        for (Ticket t : tickets) {
            csv.append(t.getId()).append(",");
            csv.append(t.getNumeroExibicao()).append(",");
            csv.append("\"").append(nullSafe(t.getTitulo())).append("\",");
            csv.append(t.getStatus()).append(",");
            csv.append(nullSafe(t.getCategoria() != null ? t.getCategoria().name() : "")).append(",");
            csv.append(nullSafe(t.getPrioridade() != null ? t.getPrioridade().name() : "")).append(",");
            csv.append("\"").append(nullSafe(t.getNomeUsuario())).append("\",");
            csv.append("\"").append(nullSafe(t.getNomeEquipe())).append("\",");
            csv.append(t.getCriadoEm()).append(",");
            csv.append(t.getFechadoEm() != null ? t.getFechadoEm() : "").append(",");
            csv.append("\"").append(nullSafe(t.getAtendenteNome())).append("\"");
            csv.append("\n");
        }
        return csv.toString();
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public void atribuirAtendente(UUID ticketId, UUID atendenteId, String atendenteNome) {
        Ticket ticket = findTicketOrThrow(ticketId);
        validarAcessoTicket(ticket);
        ticket.setAtendenteId(atendenteId);
        ticket.setAtendenteNome(atendenteNome);
        if (ticket.getStatus() == TicketStatus.ABERTO) {
            ticket.setStatus(TicketStatus.EM_ANDAMENTO);
        }
        repo.save(ticket);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public void salvarFeedback(UUID ticketId, int nota, String comentario) {
        if (nota < 1 || nota > 5) {
            throw badRequest("Nota deve ser entre 1 e 5");
        }
        Ticket ticket = findTicketOrThrow(ticketId);
        if (ticket.getStatus() != TicketStatus.FECHADO && ticket.getStatus() != TicketStatus.RESOLVIDO) {
            throw badRequest("So e possivel avaliar tickets finalizados");
        }
        if (feedbackRepo.existsByTicketId(ticketId)) {
            throw badRequest("Ticket ja foi avaliado");
        }
        TicketFeedback feedback = TicketFeedback.builder()
                .ticketId(ticketId)
                .nota(nota)
                .comentario(htmlSanitizer.sanitizeKeepNewlines(comentario))
                .build();
        feedbackRepo.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketMessageResponseDTO> listar(UUID ticketId) {
        if (ticketId == null)
            throw badRequest("ticketId e obrigatorio");

        Ticket ticket = findTicketOrThrow(ticketId);
        validarAcessoTicket(ticket);

        return messageRepo.findByTicket_IdOrderByCriadoEmAsc(ticketId).stream()
                .map(this::toMessageDTO).toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public TicketMessageResponseDTO adicionar(UUID ticketId, UUID autorUsuarioId, CreateTicketMessageRequestDTO dto) {
        if (ticketId == null)
            throw badRequest("ticketId e obrigatorio");
        if (autorUsuarioId == null)
            throw badRequest("autorUsuarioId e obrigatorio");
        if (dto == null)
            throw badRequest("Payload invalido");
        if (dto.autorTipo() == null)
            throw badRequest("autorTipo e obrigatorio");
        if (isBlank(dto.texto()))
            throw badRequest("texto e obrigatorio");

        Ticket ticket = findTicketOrThrow(ticketId);
        validarAcessoTicket(ticket);

        String autorNome = resolveAutorNome(autorUsuarioId);
        String equipeNome = nullSafe(ticket.getNomeEquipe());

        String textoSanitizado = htmlSanitizer.sanitizeKeepNewlines(dto.texto().trim());

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .autorUsuarioId(autorUsuarioId)
                .autorTipo(dto.autorTipo())
                .autorNome(autorNome)
                .equipeNome(equipeNome)
                .texto(textoSanitizado)
                .criadoEm(OffsetDateTime.now())
                .build();

        boolean isPrimeiraResposta = ticket.getPrimeiraRespostaEm() == null
                && dto.autorTipo() != br.com.fightConnect.domain.models.enums.TicketMessageAuthorType.USUARIO;

        TicketMessage saved = messageRepo.save(msg);

        if (isPrimeiraResposta) {
            ticket.setPrimeiraRespostaEm(OffsetDateTime.now());
            repo.save(ticket);
        }

        notificationHelper.notifyNewMessage(ticket, saved);

        if (ticket.getStatus() == TicketStatus.EM_ANDAMENTO) {
            ticket.setStatus(TicketStatus.AGUARDANDO_USUARIO);
            repo.save(ticket);
        }

        return toMessageDTO(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public void responderEFinalizar(UUID ticketId, UUID autorUsuarioId, FinalizarTicketRequestDTO dto) {
        if (ticketId == null)
            throw badRequest("ticketId e obrigatorio");
        if (autorUsuarioId == null)
            throw badRequest("autorUsuarioId e obrigatorio");
        if (dto == null)
            throw badRequest("Payload invalido");
        if (dto.autorTipo() == null)
            throw badRequest("autorTipo e obrigatorio");
        if (isBlank(dto.textoResposta()))
            throw badRequest("textoResposta e obrigatorio");
        if (dto.statusFinal() == null)
            throw badRequest("statusFinal e obrigatorio");
        if (!isFinalStatus(dto.statusFinal()))
            throw badRequest("statusFinal deve ser RESOLVIDO ou FECHADO");

        Ticket ticket = findTicketOrThrow(ticketId);
        validarAcessoTicket(ticket);

        TicketStatus old = ticket.getStatus();

        String autorNome = resolveAutorNome(autorUsuarioId);
        String equipeNome = nullSafe(ticket.getNomeEquipe());
        String textoSanitizado = htmlSanitizer.sanitizeKeepNewlines(dto.textoResposta().trim());

        TicketMessage msg = TicketMessage.builder()
                .ticket(ticket)
                .autorUsuarioId(autorUsuarioId)
                .autorTipo(dto.autorTipo())
                .autorNome(autorNome)
                .equipeNome(equipeNome)
                .texto(textoSanitizado)
                .criadoEm(OffsetDateTime.now())
                .build();

        TicketMessage msgSaved = messageRepo.save(msg);

        if (ticket.getPrimeiraRespostaEm() == null) {
            ticket.setPrimeiraRespostaEm(OffsetDateTime.now());
        }

        notificationHelper.notifyNewMessage(ticket, msgSaved);

        onReopenIfNeeded(ticket, old, dto.statusFinal());
        ticket.setStatus(dto.statusFinal());
        applyStatusLogic(ticket, dto.statusFinal());

        Ticket saved = repo.save(ticket);

        notificationHelper.notifyFinalAnswer(saved, old, dto.statusFinal(), msgSaved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketsList", allEntries = true)
    public void marcarComoLido(UUID ticketId, UUID usuarioId) {
        if (ticketId == null)
            throw badRequest("ticketId e obrigatorio");
        if (usuarioId == null)
            throw badRequest("usuarioId e obrigatorio");

        Ticket ticket = findTicketOrThrow(ticketId);
        validarAcessoTicket(ticket);

        var now = OffsetDateTime.now();

        Optional<TicketRead> existing = readRepo.findByTicketIdAndUsuarioId(ticketId, usuarioId);
        if (existing.isPresent()) {
            TicketRead r = existing.get();
            r.setLastReadAt(now);
            readRepo.save(r);
            return;
        }

        readRepo.save(TicketRead.builder().ticketId(ticketId).usuarioId(usuarioId).lastReadAt(now).build());
    }

    private TicketMessageResponseDTO toMessageDTO(TicketMessage m) {
        return new TicketMessageResponseDTO(
                m.getId(), m.getTicket() == null ? null : m.getTicket().getId(),
                m.getAutorUsuarioId(), m.getAutorNome(), m.getAutorTipo(),
                m.getEquipeNome(), m.getTexto(), m.getCriadoEm());
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private Ticket findTicketOrThrow(UUID id) {
        if (id == null) throw badRequest("id e obrigatorio");
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket nao encontrado"));
    }

    private Long nextNumeroTicketOrThrow() {
        try {
            return repo.nextNumeroTicket();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao gerar numero do ticket");
        }
    }

    private void onReopenIfNeeded(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
        if (oldStatus == null || newStatus == null) return;

        boolean estavaFechado = oldStatus == TicketStatus.FECHADO
                || oldStatus == TicketStatus.RESOLVIDO
                || oldStatus == TicketStatus.CANCELADO;

        boolean reabrindo = newStatus == TicketStatus.ABERTO
                || newStatus == TicketStatus.EM_ANDAMENTO
                || newStatus == TicketStatus.AGUARDANDO_USUARIO;

        if (estavaFechado && reabrindo) {
            int atual = (t.getReaberturaSeq() == null ? 0 : t.getReaberturaSeq());
            t.setReaberturaSeq(atual + 1);
            t.setFechadoEm(null);
            t.setPrimeiraRespostaEm(null);
        }
    }

    private void applyStatusLogic(Ticket ticket, TicketStatus status) {
        boolean fechando = status == TicketStatus.FECHADO
                || status == TicketStatus.RESOLVIDO
                || status == TicketStatus.CANCELADO;

        if (fechando) {
            if (ticket.getFechadoEm() == null)
                ticket.setFechadoEm(OffsetDateTime.now());
        } else {
            ticket.setFechadoEm(null);
        }
    }

    private boolean isFinalStatus(TicketStatus status) {
        return status == TicketStatus.FECHADO || status == TicketStatus.RESOLVIDO;
    }

    private void refreshNomeUsuario(Ticket ticket) {
        try {
            var info = buscarInfoUsuario(ticket.getUsuarioId());
            if (info != null && !isBlank(info.nome()))
                ticket.setNomeUsuario(info.nome());
        } catch (Exception ignored) {}
    }

    private UsuarioInfo buscarInfoUsuario(UUID usuarioId) {
        try {
            var a = apiAuthAlunoClient.buscarPorId(usuarioId);
            if (a != null) return new UsuarioInfo(a.getNome(), a.getEmail());
        } catch (Exception ignored) {}
        try {
            var p = apiAuthProfessorClient.buscarPorId(usuarioId);
            if (p != null) return new UsuarioInfo(p.getNome(), p.getEmail());
        } catch (Exception ignored) {}
        try {
            var u = apiAuthAdminClient.buscarPorId(usuarioId);
            if (u != null) return new UsuarioInfo(u.getNome(), u.getEmail());
        } catch (Exception ignored) {}
        return new UsuarioInfo("Usuario", null);
    }

    private record UsuarioInfo(String nome, String email) {}

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String resolveAutorNome(UUID autorUsuarioId) {
        if (autorUsuarioId == null) return "Usuario";
        try {
            var info = buscarInfoUsuario(autorUsuarioId);
            if (info != null && !isBlank(info.nome())) return info.nome();
        } catch (Exception ignored) {}
        return "Usuario";
    }

    private UUID getEquipeIdDoToken() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                String equipeIdStr = jwt.getClaimAsString("equipeId");
                if (equipeIdStr != null) return UUID.fromString(equipeIdStr);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isAdminDoToken() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        } catch (Exception ignored) {
            return false;
        }
    }
    private void validarAcessoTicket(Ticket ticket) {
        if (isAdminDoToken()) return;
        UUID equipeIdToken = getEquipeIdDoToken();
        if (equipeIdToken != null && !ticket.getEquipeId().equals(equipeIdToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado aos dados de outra equipe.");
        }
    }

    private TicketFotoResponseDTO toFotoDTO(TicketFoto f) {
        return new TicketFotoResponseDTO(
                f.getId(), f.getFileName(), f.getContentType(),
                f.getSizeBytes(), f.getUrl(), f.getCriadoEm());
    }

    private TicketResponseDTO toDTO(Ticket t) {
        var fotos = fotoRepo.findByTicketIdOrderByCriadoEmDesc(t.getId()).stream()
                .map(this::toFotoDTO).toList();

        return new TicketResponseDTO(
                t.getId(), t.getUsuarioId(), t.getPlanoId(), t.getEquipeId(),
                t.getTitulo(), t.getDescricao(),
                t.getNumeroTicket(), t.getReaberturaSeq(), t.getNumeroExibicao(),
                t.getStatus(), t.getCategoria(), t.getPrioridade(),
                t.getAtendenteId(), t.getAtendenteNome(),
                t.getCriadoEm(), t.getAtualizadoEm(), t.getFechadoEm(),
                t.getPrimeiraRespostaEm(),
                t.getNomeUsuario(), t.getNomeEquipe(), t.getNomePlano(), fotos);
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
