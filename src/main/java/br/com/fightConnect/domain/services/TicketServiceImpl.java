package br.com.fightConnect.domain.services;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

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
import br.com.fightConnect.domain.models.entities.TicketFoto;
import br.com.fightConnect.domain.models.entities.TicketMessage;
import br.com.fightConnect.domain.models.entities.TicketRead;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAdminClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAlunoClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthProfessorClient;
import br.com.fightConnect.infrastructure.repositories.TicketFotoRepository;
import br.com.fightConnect.infrastructure.repositories.TicketMessageRepository;
import br.com.fightConnect.infrastructure.repositories.TicketReadRepository;
import br.com.fightConnect.infrastructure.repositories.TicketRepository;
import br.com.fightConnect.infrastructure.repositories.specs.TicketSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

	// ✅ clients só pra pegar NOME/EMAIL do usuário (status change / auditoria etc)
	private final ApiAuthAlunoClient apiAuthAlunoClient;
	private final ApiAuthProfessorClient apiAuthProfessorClient;
	private final ApiAuthAdminClient apiAuthAdminClient;

	private final TicketRepository repo;
	private final TicketFotoRepository fotoRepo;
	private final TicketMessageRepository messageRepo;
	private final TicketReadRepository readRepo;

	private final TicketFotoPorEquipeComponent ticketFotoPorEquipeComponent;
	private final FileStorageService storage;
	private final MailService mailService;

	private static final List<TicketStatus> STATUS_ABERTOS = List
			.copyOf(EnumSet.of(TicketStatus.ABERTO, TicketStatus.EM_ANDAMENTO, TicketStatus.AGUARDANDO_USUARIO));

	private UUID getEquipeIdDoToken() {
		try {
			var auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
				String equipeIdStr = jwt.getClaimAsString("equipeId");
				if (equipeIdStr != null) return UUID.fromString(equipeIdStr);
			}
		} catch (Exception ignored) {}
		return null;
	}

	private boolean isAdminDoToken() {
		try {
			var auth = SecurityContextHolder.getContext().getAuthentication();
			return auth != null && auth.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
		} catch (Exception ignored) {}
		return false;
	}

	private void validarAcessoTicket(Ticket ticket) {
		if (isAdminDoToken()) return; // Admin vê tudo

		UUID equipeIdToken = getEquipeIdDoToken();

		// Se não for da mesma equipe, bloqueia
		if (equipeIdToken != null && !ticket.getEquipeId().equals(equipeIdToken)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado aos dados de outra equipe.");
		}
	}

	// ==========================
	// CREATE
	// ==========================
	@Override
	@Transactional
	public TicketResponseDTO criar(CreateTicketRequestDTO dto) {
		validateCreate(dto);

		String nomeEquipe = defaultIfBlank(dto.nomeEquipe(), null);
		String nomePlano = defaultIfBlank(dto.nomePlano(), null);

		if (isBlank(nomeEquipe))
			throw badRequest("nomeEquipe é obrigatório");
		if (isBlank(nomePlano))
			throw badRequest("nomePlano é obrigatório");

		var infoUsuario = buscarInfoUsuario(dto.usuarioId());
		String nomeUsuario = defaultIfBlank(infoUsuario.nome(), "Usuário");

		Long numeroTicket = nextNumeroTicketOrThrow();

		Ticket ticket = Ticket.builder().usuarioId(dto.usuarioId()).equipeId(dto.equipeId()).planoId(dto.planoId())
				.titulo(dto.titulo()).descricao(dto.descricao()).status(TicketStatus.ABERTO).nomeUsuario(nomeUsuario)
				.nomeEquipe(nomeEquipe).nomePlano(nomePlano).numeroTicket(numeroTicket).reaberturaSeq(0).build();

		Ticket saved = repo.save(ticket);

		ticketFotoPorEquipeComponent.salvar(saved.getId(), saved.getEquipeId(), saved.getNomeEquipe(),
				dto.fotosBase64());

		sendEmailToSupportOnCreate(saved);
		enqueuePushToSupportProfilesOnCreate(saved);

		return toDTO(saved);
	}

	private void validateCreate(CreateTicketRequestDTO dto) {
		if (dto == null)
			throw badRequest("Payload inválido");
		if (dto.usuarioId() == null)
			throw badRequest("usuarioId é obrigatório");
		if (dto.equipeId() == null)
			throw badRequest("equipeId é obrigatório");
		if (isBlank(dto.titulo()))
			throw badRequest("titulo é obrigatório");
	}

	private void enqueuePushToSupportProfilesOnCreate(Ticket saved) {
		if (saved == null)
			return;

		try {
			String ticketNumero = nullSafe(saved.getNumeroExibicao());
			String ticketId = saved.getId() == null ? "" : saved.getId().toString();
			String ticketTitulo = nullSafe(saved.getTitulo());
			String usuarioNome = nullSafe(saved.getNomeUsuario());

			String titulo = "Novo Ticket " + ticketNumero;
			String corpo = usuarioNome + " abriu um ticket: " + ticketTitulo;

			String url = "/public/suporte" + (ticketId.isBlank() ? "" : ("?ticketId=" + ticketId));
			String tag = "suporte-ticket-" + (ticketId.isBlank() ? ticketNumero : ticketId);

			mailService.notifyPushToProfiles(saved.getEquipeId(), List.of("SUPER_GESTOR", "SUPER_ADMIN"),
					"SUPORTE_TICKET_CRIADO", titulo, corpo,
					url, tag, ticketId, ticketNumero, ticketTitulo, usuarioNome, null);

		} catch (Exception e) {
			System.out.println("❌ Falha ao enfileirar PUSH do ticket " + saved.getId() + ": " + e.getMessage());
		}
	}

	// ==========================
	// UPDATE
	// ==========================
	@Override
	@Transactional
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
				ticket.setTitulo(dto.titulo());
			if (dto.descricao() != null)
				ticket.setDescricao(dto.descricao());
		}

		refreshNomeUsuario(ticket);

		Ticket saved = repo.save(ticket);

		if (dto != null && dto.fotosBase64() != null && !dto.fotosBase64().isEmpty()) {
			ticketFotoPorEquipeComponent.salvar(saved.getId(), saved.getEquipeId(), saved.getNomeEquipe(),
					dto.fotosBase64());
		}

		return toDTO(saved);
	}

	// ==========================
	// UPDATE STATUS (sem resposta)
	// ==========================
	@Override
	@Transactional
	public TicketResponseDTO atualizarStatus(UUID id, TicketStatus status) {
		if (status == null)
			throw badRequest("status é obrigatório");

		// ✅ Regra: não finalizar sem mensagem (use responderEFinalizar)
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
			sendEmailToUserOnStatusChange(saved, old, status);
		}

		return toDTO(saved);
	}

	private boolean isFinalStatus(TicketStatus status) {
		return status == TicketStatus.FECHADO || status == TicketStatus.RESOLVIDO;
	}

	// ==========================
	// DELETE
	// ==========================
	@Override
	@Transactional
	public void deletar(UUID id) {
		Ticket ticket = repo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));

		validarAcessoTicket(ticket);

		List<TicketFoto> fotos = fotoRepo.findByTicketIdOrderByCriadoEmDesc(ticket.getId());
		for (TicketFoto f : fotos) {
			try {
				if (!isBlank(f.getStorageKey()))
					storage.delete(f.getStorageKey());
			} catch (Exception ignored) {
			}
		}
		if (!fotos.isEmpty())
			fotoRepo.deleteAll(fotos);

		repo.delete(ticket);
	}

	// ==========================
	// GET / LIST
	// ==========================
	@Override
	@Transactional(readOnly = true)
	public TicketResponseDTO buscarPorId(UUID id) {
		Ticket ticket = findTicketOrThrow(id);
		validarAcessoTicket(ticket);
		return toDTO(ticket);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TicketResponseDTO> listarTodos(boolean apenasAbertos, List<TicketStatus> status, Pageable pageable) {
		// Se não for admin, filtra pela equipe do token
		UUID equipeIdToken = isAdminDoToken() ? null : getEquipeIdDoToken();
		
		List<TicketStatus> filtroStatus = (status != null && !status.isEmpty()) ? status
				: (apenasAbertos ? STATUS_ABERTOS : null);

		Specification<Ticket> spec = TicketSpecifications.filtro(null, equipeIdToken, filtroStatus, null, null);
		return repo.findAll(spec, pageable).map(this::toDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TicketResponseDTO> listarPorUsuario(UUID usuarioId, boolean apenasAbertos, List<TicketStatus> status,
			Pageable pageable) {
		if (usuarioId == null)
			throw badRequest("usuarioId é obrigatório");

		// Se não for admin, garante que o usuário logado só veja os seus próprios tickets ou seja da mesma equipe
		// Por segurança, vamos apenas filtrar pelo usuarioId fornecido, mas o controller deve garantir que o usuarioId é o logado.
		
		List<TicketStatus> filtro = (status != null && !status.isEmpty()) ? status
				: (apenasAbertos ? STATUS_ABERTOS : null);

		Page<Ticket> page = (filtro == null) ? repo.findByUsuarioId(usuarioId, pageable)
				: repo.findByUsuarioIdAndStatusIn(usuarioId, filtro, pageable);

		return page.map(this::toDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TicketResponseDTO> listarFiltrado(UUID usuarioId, UUID equipeId, boolean apenasAbertos,
			List<TicketStatus> status, OffsetDateTime dataInicio, OffsetDateTime dataFim, Pageable pageable) {
		
		// Multi-tenancy check
		UUID equipeIdToken = isAdminDoToken() ? null : getEquipeIdDoToken();
		UUID equipeParaFiltrar = equipeId;
		
		if (equipeIdToken != null) {
			if (equipeId != null && !equipeId.equals(equipeIdToken)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é permitido listar tickets de outra equipe.");
			}
			equipeParaFiltrar = equipeIdToken;
		}

		List<TicketStatus> filtroStatus = (status != null && !status.isEmpty()) ? status
				: (apenasAbertos ? STATUS_ABERTOS : null);

		Specification<Ticket> spec = TicketSpecifications.filtro(usuarioId, equipeParaFiltrar, filtroStatus, dataInicio,
				dataFim);

		return repo.findAll(spec, pageable).map(this::toDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TicketResponseDTO> listarFiltrado(UUID usuarioId, UUID equipeId, UUID viewerId, boolean apenasAbertos,
			List<TicketStatus> status, OffsetDateTime dataInicio, OffsetDateTime dataFim, Pageable pageable) {
		// Por enquanto viewerId não altera a lógica de filtro, apenas delegamos para o método principal
		return listarFiltrado(usuarioId, equipeId, apenasAbertos, status, dataInicio, dataFim, pageable);
	}

	// ==========================
	// ✅ MENSAGENS (ticket_messages)
	// ==========================
	@Override
	@Transactional(readOnly = true)
	public List<TicketMessageResponseDTO> listar(UUID ticketId) {
		if (ticketId == null)
			throw badRequest("ticketId é obrigatório");

		Ticket ticket = findTicketOrThrow(ticketId);
		validarAcessoTicket(ticket);

		return messageRepo.findByTicketIdOrderByCriadoEmAsc(ticketId).stream().map(this::toMessageDTO).toList();
	}

	@Override
	@Transactional
	public TicketMessageResponseDTO adicionar(UUID ticketId, UUID autorUsuarioId, CreateTicketMessageRequestDTO dto) {
		if (ticketId == null)
			throw badRequest("ticketId é obrigatório");
		if (autorUsuarioId == null)
			throw badRequest("autorUsuarioId é obrigatório");
		if (dto == null)
			throw badRequest("Payload inválido");
		if (dto.autorTipo() == null)
			throw badRequest("autorTipo é obrigatório");
		if (isBlank(dto.texto()))
			throw badRequest("texto é obrigatório");

		Ticket ticket = findTicketOrThrow(ticketId);
		validarAcessoTicket(ticket);

		String autorNome = resolveAutorNome(autorUsuarioId); // ✅ snapshot
		String equipeNome = nullSafe(ticket.getNomeEquipe()); // ✅ snapshot

		TicketMessage msg = TicketMessage.builder().ticket(ticket).autorUsuarioId(autorUsuarioId)
				.autorTipo(dto.autorTipo()).autorNome(autorNome).equipeNome(equipeNome).texto(dto.texto().trim())
				.criadoEm(OffsetDateTime.now()).build();

		TicketMessage saved = messageRepo.save(msg);

		// ✅ PUSH: nova mensagem (vai pra fila, worker dispara)
		enqueuePushNewMessage(ticket, saved);

		if (ticket.getStatus() == TicketStatus.EM_ANDAMENTO) {
			ticket.setStatus(TicketStatus.AGUARDANDO_USUARIO);
			repo.save(ticket);
		}

		return toMessageDTO(saved);
	}

	@Override
	@Transactional
	public void responderEFinalizar(UUID ticketId, UUID autorUsuarioId, FinalizarTicketRequestDTO dto) {
		if (ticketId == null)
			throw badRequest("ticketId é obrigatório");
		if (autorUsuarioId == null)
			throw badRequest("autorUsuarioId é obrigatório");
		if (dto == null)
			throw badRequest("Payload inválido");
		if (dto.autorTipo() == null)
			throw badRequest("autorTipo é obrigatório");
		if (isBlank(dto.textoResposta()))
			throw badRequest("textoResposta é obrigatório");
		if (dto.statusFinal() == null)
			throw badRequest("statusFinal é obrigatório");
		if (!isFinalStatus(dto.statusFinal()))
			throw badRequest("statusFinal deve ser RESOLVIDO ou FECHADO");

		Ticket ticket = findTicketOrThrow(ticketId);
		validarAcessoTicket(ticket);
		
		TicketStatus old = ticket.getStatus();

		String autorNome = resolveAutorNome(autorUsuarioId); // ✅ snapshot
		String equipeNome = nullSafe(ticket.getNomeEquipe()); // ✅ snapshot

		// 1) cria mensagem final
		TicketMessage msg = TicketMessage.builder().ticket(ticket).autorUsuarioId(autorUsuarioId)
				.autorTipo(dto.autorTipo()).autorNome(autorNome).equipeNome(equipeNome)
				.texto(dto.textoResposta().trim()).criadoEm(OffsetDateTime.now()).build();

		TicketMessage msgSaved = messageRepo.save(msg);

		// ✅ PUSH: nova mensagem (vai pra fila, worker dispara)
		enqueuePushNewMessage(ticket, msgSaved);

		// 2) atualiza status
		onReopenIfNeeded(ticket, old, dto.statusFinal());
		ticket.setStatus(dto.statusFinal());
		applyStatusLogic(ticket, dto.statusFinal());

		Ticket saved = repo.save(ticket);

		// 3) notifica usuário com a RESPOSTA
		sendEmailToUserOnFinalAnswer(saved, old, dto.statusFinal(), msgSaved);
	}

	@Override
	@Transactional
	public void marcarComoLido(UUID ticketId, UUID usuarioId) {
		if (ticketId == null)
			throw badRequest("ticketId é obrigatório");
		if (usuarioId == null)
			throw badRequest("usuarioId é obrigatório");

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
		return new TicketMessageResponseDTO(m.getId(), m.getTicket() == null ? null : m.getTicket().getId(),
				m.getAutorUsuarioId(), m.getAutorNome(), m.getAutorTipo(), m.getEquipeNome(), m.getTexto(),
				m.getCriadoEm());
	}

	// ==========================
	// Helpers
	// ==========================
	private static ResponseStatusException badRequest(String msg) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
	}

	private Ticket findTicketOrThrow(UUID id) {
		if (id == null)
			throw badRequest("id é obrigatório");
		return repo.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));
	}

	private Long nextNumeroTicketOrThrow() {
		try {
			return repo.nextNumeroTicket();
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Falha ao gerar número do ticket (nextNumeroTicket não disponível/configurado)");
		}
	}

	private void onReopenIfNeeded(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
		if (oldStatus == null || newStatus == null)
			return;

		boolean estavaFechado = oldStatus == TicketStatus.FECHADO || oldStatus == TicketStatus.RESOLVIDO
				|| oldStatus == TicketStatus.CANCELADO;

		boolean reabrindo = newStatus == TicketStatus.ABERTO || newStatus == TicketStatus.EM_ANDAMENTO
				|| newStatus == TicketStatus.AGUARDANDO_USUARIO;

		if (estavaFechado && reabrindo) {
			int atual = (t.getReaberturaSeq() == null ? 0 : t.getReaberturaSeq());
			t.setReaberturaSeq(atual + 1);
			t.setFechadoEm(null);
		}
	}

	private void applyStatusLogic(Ticket ticket, TicketStatus status) {
		boolean fechando = status == TicketStatus.FECHADO || status == TicketStatus.RESOLVIDO
				|| status == TicketStatus.CANCELADO;

		if (fechando) {
			if (ticket.getFechadoEm() == null)
				ticket.setFechadoEm(OffsetDateTime.now());
		} else {
			ticket.setFechadoEm(null);
		}
	}

	private void refreshNomeUsuario(Ticket ticket) {
		try {
			var info = buscarInfoUsuario(ticket.getUsuarioId());
			if (info != null && !isBlank(info.nome()))
				ticket.setNomeUsuario(info.nome());
		} catch (Exception ignored) {
		}
	}

	private UsuarioInfo buscarInfoUsuario(UUID usuarioId) {
		try {
			var a = apiAuthAlunoClient.buscarPorId(usuarioId);
			if (a != null)
				return new UsuarioInfo(a.getNome(), a.getEmail());
		} catch (Exception ignored) {
		}

		try {
			var p = apiAuthProfessorClient.buscarPorId(usuarioId);
			if (p != null)
				return new UsuarioInfo(p.getNome(), p.getEmail());
		} catch (Exception ignored) {
		}

		try {
			var u = apiAuthAdminClient.buscarPorId(usuarioId);
			if (u != null)
				return new UsuarioInfo(u.getNome(), u.getEmail());
		} catch (Exception ignored) {
		}

		return new UsuarioInfo("Usuário", null);
	}

	private record UsuarioInfo(String nome, String email) {
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static String defaultIfBlank(String value, String fallback) {
		return isBlank(value) ? fallback : value;
	}

	private String resolveAutorNome(UUID autorUsuarioId) {
		if (autorUsuarioId == null)
			return "Usuário";
		try {
			var info = buscarInfoUsuario(autorUsuarioId);
			if (info != null && !isBlank(info.nome()))
				return info.nome();
		} catch (Exception ignored) {
		}
		return "Usuário";
	}

	// ==========================
	// E-mails / PUSH (fila)
	// ==========================
	private void sendEmailToSupportOnCreate(Ticket t) {
		String subject = "Novo Ticket " + nullSafe(t.getNumeroExibicao());
		String body = buildSupportNewTicketText(t);

		try {
			mailService.notifySupportNewTicket(t.getEquipeId(), subject, body);
		} catch (Exception ignored) {
		}
	}

	private void sendEmailToUserOnStatusChange(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
		String email = resolveUserEmail(t.getUsuarioId());
		if (isBlank(email))
			return;

		String subject = "Atualização do Ticket " + nullSafe(t.getNumeroExibicao()) + " - "
				+ (newStatus == null ? "" : newStatus.name());
		String body = buildUserStatusChangeText(t, oldStatus, newStatus);

		try {
			mailService.notifyUserStatusChange(t.getEquipeId(), email, subject, body);
		} catch (Exception ignored) {
		}
	}

	private void sendEmailToUserOnFinalAnswer(Ticket t, TicketStatus oldStatus, TicketStatus newStatus,
			TicketMessage msg) {
		String email = resolveUserEmail(t.getUsuarioId());
		if (isBlank(email))
			return;

		String subject = "Ticket " + nullSafe(t.getNumeroExibicao()) + " - "
				+ (newStatus == null ? "" : newStatus.name());
		String body = buildUserFinalAnswerText(t, oldStatus, newStatus, msg);

		try {
			// reaproveita o pipeline de e-mail do worker (fila)
			mailService.notifyUserStatusChange(t.getEquipeId(), email, subject, body);
		} catch (Exception ignored) {
		}
	}

	private String resolveUserEmail(UUID usuarioId) {
		try {
			var info = buscarInfoUsuario(usuarioId);
			return info != null ? info.email() : null;
		} catch (Exception ignored) {
			return null;
		}
	}

	private void enqueuePushNewMessage(Ticket ticket, TicketMessage msg) {
		if (ticket == null || msg == null)
			return;

		final String ticketNumero = nullSafe(ticket.getNumeroExibicao());
		final String ticketId = (ticket.getId() == null) ? "" : ticket.getId().toString();
		final String ticketTitulo = nullSafe(ticket.getTitulo());

		final UUID autorId = msg.getAutorUsuarioId();
		final UUID donoId = ticket.getUsuarioId();

		final boolean autorEhDono = autorId != null && donoId != null && autorId.equals(donoId);

		final String autorNome = nullSafe(msg.getAutorNome());

		String preview = nullSafe(msg.getTexto()).replace("\n", " ").trim();
		if (preview.length() > 110)
			preview = preview.substring(0, 110) + "...";

		final String titulo = "Nova mensagem • Ticket " + ticketNumero;

		// ✅ o worker monta "autor + ': ' + preview", então aqui manda só o preview
		final String corpo = preview;

		final String url = "/public/suporte" + (ticketId.isBlank() ? "" : ("?ticketId=" + ticketId));
		final String tag = "suporte-ticket-" + (ticketId.isBlank() ? ticketNumero : ticketId);

		log.info(
				"🧭 SUPORTE PUSH ROUTE ticketId={} ticketNumero={} autorId={} donoId={} autorTipo={} -> autorEhDono={}",
				ticket.getId(), ticketNumero, autorId, donoId,
				msg.getAutorTipo() == null ? "null" : msg.getAutorTipo().name(), autorEhDono);

		try {
			if (autorEhDono) {
				log.info("➡️ ROUTE: autor é DONO -> enviar para PERFIS SUPER_GESTOR/SUPER_ADMIN");

				mailService.notifyPushToProfile(ticket.getEquipeId(), "SUPER_GESTOR", "SUPORTE_NOVA_MENSAGEM", titulo,
						corpo, url, tag, ticketId, ticketNumero, ticketTitulo, autorNome, null);

				mailService.notifyPushToProfile(ticket.getEquipeId(), "SUPER_ADMIN", "SUPORTE_NOVA_MENSAGEM", titulo,
						corpo, url, tag, ticketId, ticketNumero, ticketTitulo, autorNome, null);

				return;
			}

			log.info("➡️ ROUTE: autor NÃO é dono -> enviar para USUARIO dono do ticket");

			mailService.notifyPushToUser(ticket.getEquipeId(), donoId, "SUPORTE_NOVA_MENSAGEM", titulo, corpo, url, tag,
					ticketId, ticketNumero, ticketTitulo, autorNome, null);

		} catch (Exception e) {
			log.error("❌ Falha ao enfileirar PUSH nova mensagem ticketId={} autorId={} donoId={} -> {}", ticket.getId(),
					autorId, donoId, e.getMessage(), e);
		}
	}

	private String buildPushBodyNewMessage(Ticket ticket, TicketMessage msg) {
		String autor = nullSafe(msg.getAutorNome()).trim();
		String texto = nullSafe(msg.getTexto()).replace("\n", " ").trim();

		if (texto.length() > 120)
			texto = texto.substring(0, 117) + "...";

		String tituloTicket = nullSafe(ticket.getTitulo()).trim();

		String corpo;
		if (!autor.isBlank()) {
			corpo = autor + ": " + texto;
		} else {
			corpo = texto;
		}

		// opcional: se quiser aparecer o título do ticket em cima
		if (!tituloTicket.isBlank()) {
			corpo = tituloTicket + " • " + corpo;
		}

		if (corpo.length() > 160)
			corpo = corpo.substring(0, 157) + "...";
		return corpo;
	}

	private static String nullSafe(String s) {
		return s == null ? "" : s;
	}

	// ==========================
	// DTO Mapping
	// ==========================
	private TicketFotoResponseDTO toFotoDTO(TicketFoto f) {
		return new TicketFotoResponseDTO(f.getId(), f.getFileName(), f.getContentType(), f.getSizeBytes(), f.getUrl(),
				f.getCriadoEm());
	}

	private TicketResponseDTO toDTO(Ticket t) {
		var fotos = fotoRepo.findByTicketIdOrderByCriadoEmDesc(t.getId()).stream().map(this::toFotoDTO).toList();

		return new TicketResponseDTO(t.getId(), t.getUsuarioId(), t.getPlanoId(), t.getEquipeId(), t.getTitulo(),
				t.getDescricao(),

				t.getNumeroTicket(), t.getReaberturaSeq(), t.getNumeroExibicao(),

				t.getStatus(), t.getCriadoEm(), t.getAtualizadoEm(), t.getFechadoEm(),

				t.getNomeUsuario(), t.getNomeEquipe(), t.getNomePlano(),

				fotos);
	}

	private String buildSupportNewTicketText(Ticket t) {
		String numero = nullSafe(t.getNumeroExibicao());
		String id = t.getId() == null ? "" : t.getId().toString();
		String equipe = nullSafe(t.getNomeEquipe());
		String usuario = nullSafe(t.getNomeUsuario());
		String titulo = nullSafe(t.getTitulo());
		String status = t.getStatus() == null ? "" : t.getStatus().name();

		String desc = t.getDescricao() == null ? "" : t.getDescricao().trim();
		if (desc.isBlank())
			desc = "Sem descrição.";

		return """
				FightConnect • Novo Ticket

				Ticket: %s
				Título: %s
				Equipe: %s
				Usuário: %s
				Status: %s
				ID: %s

				Descrição:
				%s

				— Enviado automaticamente pelo sistema FightConnect.
				""".formatted(numero, titulo, equipe, usuario, status, id, desc);
	}

	private String buildUserStatusChangeText(Ticket t, TicketStatus oldStatus, TicketStatus newStatus) {
		String numero = nullSafe(t.getNumeroExibicao());
		String titulo = nullSafe(t.getTitulo());
		String equipe = nullSafe(t.getNomeEquipe());
		String oldS = oldStatus == null ? "" : oldStatus.name();
		String newS = newStatus == null ? "" : newStatus.name();

		return """
				FightConnect • Suporte

				Seu ticket foi atualizado.

				Ticket: %s
				Título: %s
				Equipe: %s
				Status anterior: %s
				Novo status: %s

				— Enviado automaticamente pelo sistema FightConnect.
				""".formatted(numero, titulo, equipe, oldS, newS);
	}

	private String buildUserFinalAnswerText(Ticket t, TicketStatus oldStatus, TicketStatus newStatus,
			TicketMessage msg) {
		String numero = nullSafe(t.getNumeroExibicao());
		String titulo = nullSafe(t.getTitulo());
		String equipe = nullSafe(t.getNomeEquipe());
		String status = newStatus == null ? "" : newStatus.name();

		String resposta = (msg == null || msg.getTexto() == null) ? "" : msg.getTexto().trim();
		if (resposta.isBlank())
			resposta = "Sem detalhes.";

		return """
				FightConnect • Suporte

				Seu ticket foi finalizado.

				Ticket: %s
				Título: %s
				Equipe: %s
				Novo status: %s

				Resposta do suporte:
				%s

				— Enviado automaticamente pelo sistema FightConnect.
				""".formatted(numero, titulo, equipe, status, resposta);
	}

}
