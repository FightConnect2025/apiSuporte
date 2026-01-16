package br.com.vibetex.domain.services;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.vibetex.domain.components.TicketFotoComponent;
import br.com.vibetex.domain.contracts.services.TicketService;
import br.com.vibetex.domain.models.dtos.CreateTicketRequestDTO;
import br.com.vibetex.domain.models.dtos.TicketFotoResponseDTO;
import br.com.vibetex.domain.models.dtos.TicketResponseDTO;
import br.com.vibetex.domain.models.dtos.UpdateTicketRequestDTO;
import br.com.vibetex.domain.models.entities.Ticket;
import br.com.vibetex.domain.models.enums.TicketStatus;
import br.com.vibetex.infrastructure.clients.controlapp.ControlAppClient.ControlAppUsuarioClient;
import br.com.vibetex.infrastructure.repositories.TicketFotoRepository;
import br.com.vibetex.infrastructure.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;

/**
 * Regras de e-mail: 1) Ao criar ticket -> envia para SUPORTE. 2) Ao mudar
 * status -> envia para USUÁRIO (se houver e-mail disponível).
 *
 * Obs: para enviar pro usuário, o DTO retornado pelo ControlApp precisa ter
 * getEmail().
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

	private final ControlAppUsuarioClient controlAppUsuarioClient;
	private final TicketRepository repo;
	private final TicketFotoRepository fotoRepo;

	// ✅ componente dedicado pra fotos
	private final TicketFotoComponent ticketFotoComponent;

	// ✅ e-mail
	private final MailService mailService;

	private static final List<TicketStatus> STATUS_ABERTOS = List
			.copyOf(EnumSet.of(TicketStatus.ABERTO, TicketStatus.EM_ANDAMENTO, TicketStatus.AGUARDANDO_USUARIO));

	// ==================================
	// ✅ CRIAR (COM FOTOS NO MESMO POST)
	// ==================================
	@Override
	@Transactional
	public TicketResponseDTO criar(CreateTicketRequestDTO dto) {
	    UUID usuarioId = dto.usuarioId();
	    var usuario = controlAppUsuarioClient.getUsuarioById(usuarioId);

	    // ✅ validações mínimas pra não estourar NOT NULL
	    if (dto.numeroVistoria() == null || dto.numeroVistoria().isBlank()) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "numeroVistoria é obrigatório");
	    }
	    if (dto.titulo() == null || dto.titulo().isBlank()) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titulo é obrigatório");
	    }

	    // ✅ fallback seguro (ControlApp pode vir sem nome/empresa)
	    String nomeUsuario = (usuario != null && usuario.getNome() != null && !usuario.getNome().isBlank())
	            ? usuario.getNome()
	            : "Usuário";

	    String nomeEmpresa = (usuario != null && usuario.getNomeDaEmpresa() != null && !usuario.getNomeDaEmpresa().isBlank())
	            ? usuario.getNomeDaEmpresa()
	            : "Empresa não informada";

	    // ✅ número do ticket (assumindo que você já tenha isso na entidade)
	    // Se ainda NÃO tiver a geração pronta, remova essas 2 linhas.
	    Long numeroTicket = repo.nextNumeroTicket(); // precisa existir no repo (query nativa)
	    int reaberturaSeq = 0;

	    var ticket = Ticket.builder()
	            .usuarioId(usuarioId)
	            .vistoriaId(dto.vistoriaId())
	            .numeroVistoria(dto.numeroVistoria())
	            .titulo(dto.titulo())
	            .descricao(dto.descricao())
	            .status(TicketStatus.ABERTO)
	            .nomeUsuario(nomeUsuario)
	            .nomeEmpresa(nomeEmpresa) 
	            .numeroTicket(numeroTicket)      // ✅ novo
	            .reaberturaSeq(reaberturaSeq)    // ✅ novo
	            .build();

	    var saved = repo.save(ticket);

	    // ✅ salva fotos base64 em /images/<empresa>/<fotoId>.<ext>
	    // usa nomeEmpresa já tratado (não null)
	    ticketFotoComponent.salvarBase64(saved.getId(), nomeEmpresa, dto.fotosBase64());

	    // ✅ EMAIL: avisa suporte que um novo ticket foi criado
	    sendEmailToSupportOnCreate(saved);

	    return toDTO(saved);
	}


	@Override
	@Transactional
	public TicketResponseDTO atualizar(UUID id, UpdateTicketRequestDTO dto) {
		var ticket = findTicketOrThrow(id);

		// ✅ atualiza apenas dados editáveis (não status)
		applyUpdate(ticket, dto);

		// ✅ sempre atualizar nome/empresa no banco conforme usuário no ControlApp
		refreshNomeEmpresa(ticket);

		var saved = repo.save(ticket);
		return toDTO(saved);
	}

	@Override
	@Transactional
	public TicketResponseDTO atualizarStatus(UUID id, TicketStatus status) {
		var ticket = findTicketOrThrow(id);

		TicketStatus oldStatus = ticket.getStatus();

		ticket.setStatus(status);
		applyStatusLogic(ticket, status);

		var saved = repo.save(ticket);

		if (status != null && oldStatus != status) {
			sendEmailToUserOnStatusChange(saved, oldStatus, status);
		}

		return toDTO(saved);
	}

	@Override
	@Transactional
	public void deletar(UUID id) {
		if (!repo.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado");
		}
		repo.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public TicketResponseDTO buscarPorId(UUID id) {
		var ticket = findTicketOrThrow(id);
		return toDTO(ticket);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TicketResponseDTO> listarPorUsuario(UUID usuarioId, boolean apenasAbertos, List<TicketStatus> status,
			Pageable pageable) {

		List<TicketStatus> statusFiltro = (status != null && !status.isEmpty()) ? status
				: (apenasAbertos ? STATUS_ABERTOS : null);

		Page<Ticket> page = (statusFiltro == null) ? repo.findByUsuarioId(usuarioId, pageable)
				: repo.findByUsuarioIdAndStatusIn(usuarioId, statusFiltro, pageable);

		return page.map(this::toDTO);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<TicketResponseDTO> listarTodos(boolean apenasAbertos, List<TicketStatus> status, Pageable pageable) {

	    List<TicketStatus> statusFiltro = (status != null && !status.isEmpty())
	            ? status
	            : (apenasAbertos ? STATUS_ABERTOS : null);

	    Page<Ticket> page = (statusFiltro == null)
	            ? repo.findAll(pageable)
	            : repo.findByStatusIn(statusFiltro, pageable);

	    return page.map(this::toDTO);
	}


	// ==========================
	// Helpers (E-mail)
	// ==========================

	private void sendEmailToSupportOnCreate(Ticket t) {
		String subject = "Novo Ticket #" + t.getId();

		String desc = t.getDescricao() == null ? "" : t.getDescricao();
		desc = escapeHtml(desc).replace("\n", "<br/>");

		String html = """
				    <h2>Novo Ticket Criado</h2>
				    <p><b>ID:</b> %s</p>
				    <p><b>Empresa:</b> %s</p>
				    <p><b>Usuário:</b> %s</p>
				    <p><b>Título:</b> %s</p>
				    <p><b>Status:</b> %s</p>
				    <p><b>Descrição:</b><br/>%s</p>
				""".formatted(t.getId(), safe(t.getNomeEmpresa()), safe(t.getNomeUsuario()), safe(t.getTitulo()),
				t.getStatus(), desc);

		try {
			mailService.notifySupportNewTicket(subject, html);
		} catch (Exception ignored) {
			// não quebra criação do ticket por falha de e-mail
		}
	}

	private void sendEmailToUserOnStatusChange(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
		// Busca usuário no ControlApp para pegar email
		var usuario = controlAppUsuarioClient.getUsuarioById(t.getUsuarioId());

		String userEmail = null;
		try {
			// ✅ precisa existir no DTO
			userEmail = usuario.getEmail();
		} catch (Exception ignored) {
			// se não tiver email no DTO ainda, não envia
		}

		if (userEmail == null || userEmail.isBlank())
			return;

		String subject = "Atualização do Ticket #" + t.getId() + " - " + newStatus;

		String html = """
				    <h2>Seu ticket foi atualizado</h2>
				    <p><b>ID:</b> %s</p>
				    <p><b>Título:</b> %s</p>
				    <p><b>Empresa:</b> %s</p>
				    <p><b>Status anterior:</b> %s</p>
				    <p><b>Novo status:</b> %s</p>
				""".formatted(t.getId(), safe(t.getTitulo()), safe(t.getNomeEmpresa()), oldStatus, newStatus);

		try {
			mailService.notifyUserStatusChange(userEmail, subject, html);
		} catch (Exception ignored) {
			// não quebra atualização do ticket por falha de e-mail
		}
	}

	private static String safe(String s) {
		return s == null ? "" : escapeHtml(s);
	}

	private static String escapeHtml(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

	// ==========================
	// Helpers (Ticket)
	// ==========================
	private void onReopenIfNeeded(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
		boolean estavaFechado = oldStatus == TicketStatus.FECHADO || oldStatus == TicketStatus.RESOLVIDO
				|| oldStatus == TicketStatus.CANCELADO;

		boolean reabrindo = newStatus == TicketStatus.ABERTO || newStatus == TicketStatus.EM_ANDAMENTO
				|| newStatus == TicketStatus.AGUARDANDO_USUARIO;

		if (estavaFechado && reabrindo) {
			t.setReaberturaSeq((t.getReaberturaSeq() == null ? 0 : t.getReaberturaSeq()) + 1);
			t.setFechadoEm(null);
		}
	}

	private Ticket findTicketOrThrow(UUID id) {
		return repo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));
	}

	private void applyUpdate(Ticket ticket, UpdateTicketRequestDTO dto) {
		if (dto.vistoriaId() != null)
			ticket.setVistoriaId(dto.vistoriaId());

		if (dto.titulo() != null && !dto.titulo().isBlank())
			ticket.setTitulo(dto.titulo());

		if (dto.numeroVistoria() != null && !dto.numeroVistoria().isBlank())
			ticket.setNumeroVistoria(dto.numeroVistoria());

		if (dto.descricao() != null && !dto.descricao().isBlank())
			ticket.setDescricao(dto.descricao());

		// ✅ fotos (se vier) — se você quer permitir adicionar fotos no update
		if (dto.fotosBase64() != null && !dto.fotosBase64().isEmpty()) {
			// aqui precisa do nome da empresa pra salvar no caminho certo
			// (garante que refreshNomeEmpresa() já foi chamado antes, ou usa o que já está
			// no ticket)
			ticketFotoComponent.salvarBase64(ticket.getId(), ticket.getNomeEmpresa(), dto.fotosBase64());
		}
	}

	private void applyStatusLogic(Ticket ticket, TicketStatus status) {
		if (status == null)
			return;

		boolean fechando = status == TicketStatus.FECHADO || status == TicketStatus.RESOLVIDO
				|| status == TicketStatus.CANCELADO;

		if (fechando) {
			if (ticket.getFechadoEm() == null)
				ticket.setFechadoEm(OffsetDateTime.now());
		} else {
			ticket.setFechadoEm(null);
		}
	} 

	private void refreshNomeEmpresa(Ticket ticket) {
		var usuario = controlAppUsuarioClient.getUsuarioById(ticket.getUsuarioId());
		ticket.setNomeUsuario(usuario.getNome());
		ticket.setNomeEmpresa(usuario.getNomeDaEmpresa());
	}

	private TicketFotoResponseDTO toFotoDTO(br.com.vibetex.domain.models.entities.TicketFoto f) {
		return new TicketFotoResponseDTO(f.getId(), f.getFileName(), f.getContentType(), f.getSizeBytes(), f.getUrl(),
				f.getCriadoEm());
	}

	private TicketResponseDTO toDTO(Ticket t) {
		var fotos = fotoRepo.findByTicketIdOrderByCriadoEmDesc(t.getId()).stream().map(this::toFotoDTO).toList();

		return new TicketResponseDTO(t.getId(), t.getUsuarioId(), t.getVistoriaId(), t.getTitulo(), t.getDescricao(),
				t.getNumeroVistoria(),

				// ✅ novos
				t.getNumeroTicket(), t.getReaberturaSeq(), t.getNumeroExibicao(),

				t.getStatus(), t.getCriadoEm(), t.getAtualizadoEm(), t.getFechadoEm(), t.getNomeUsuario(),
				t.getNomeEmpresa(), fotos);
	}

}
