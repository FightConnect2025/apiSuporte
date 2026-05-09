package br.com.fightConnect.domain.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.com.fightConnect.domain.components.TicketFotoPorEquipeComponent;
import br.com.fightConnect.domain.contracts.services.FileStorageService;
import br.com.fightConnect.domain.models.dtos.CreateTicketMessageRequestDTO;
import br.com.fightConnect.domain.models.dtos.CreateTicketRequestDTO;
import br.com.fightConnect.domain.models.dtos.FinalizarTicketRequestDTO;
import br.com.fightConnect.domain.models.entities.Ticket;
import br.com.fightConnect.domain.models.entities.TicketMessage;
import br.com.fightConnect.domain.models.entities.TicketRead;
import br.com.fightConnect.domain.models.enums.TicketCategoria;
import br.com.fightConnect.domain.models.enums.TicketMessageAuthorType;
import br.com.fightConnect.domain.models.enums.TicketPrioridade;
import br.com.fightConnect.domain.models.enums.TicketStatus;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAdminClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthAlunoClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.ApiAuthClient.ApiAuthProfessorClient;
import br.com.fightConnect.infrastructure.repositories.TicketFeedbackRepository;
import br.com.fightConnect.infrastructure.repositories.TicketFotoRepository;
import br.com.fightConnect.infrastructure.repositories.TicketMessageRepository;
import br.com.fightConnect.infrastructure.repositories.TicketReadRepository;
import br.com.fightConnect.infrastructure.repositories.TicketRepository;
import br.com.fightConnect.infrastructure.utils.HtmlSanitizer;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private ApiAuthAlunoClient apiAuthAlunoClient;
    @Mock private ApiAuthProfessorClient apiAuthProfessorClient;
    @Mock private ApiAuthAdminClient apiAuthAdminClient;

    @Mock private TicketRepository repo;
    @Mock private TicketFotoRepository fotoRepo;
    @Mock private TicketMessageRepository messageRepo;
    @Mock private TicketReadRepository readRepo;
    @Mock private TicketFeedbackRepository feedbackRepo;

    @Mock private TicketFotoPorEquipeComponent ticketFotoPorEquipeComponent;
    @Mock private FileStorageService storage;
    @Mock private MailService mailService;
    @Mock private TicketNotificationHelper notificationHelper;

    @Spy
    private HtmlSanitizer htmlSanitizer = new HtmlSanitizer();

    @InjectMocks
    private TicketServiceImpl service;

    private UUID ticketId;
    private UUID usuarioId;
    private UUID equipeId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        equipeId = UUID.randomUUID();

        ticket = Ticket.builder()
                .id(ticketId)
                .usuarioId(usuarioId)
                .equipeId(equipeId)
                .titulo("Teste Ticket")
                .descricao("Descricao de teste")
                .status(TicketStatus.ABERTO)
                .nomeUsuario("Usuario Teste")
                .nomeEquipe("Equipe Teste")
                .nomePlano("Plano Teste")
                .numeroTicket(1L)
                .reaberturaSeq(0)
                .criadoEm(OffsetDateTime.now())
                .atualizadoEm(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Criacao de ticket")
    class CriarTicket {

        @Test
        @DisplayName("Deve criar ticket com sucesso")
        void deveCriarTicket() {
            CreateTicketRequestDTO dto = new CreateTicketRequestDTO(
                    usuarioId, equipeId, null, "Plano Teste",
                    "Equipe Teste", "Meu titulo", "Minha descricao",
                    null, TicketCategoria.DUVIDA, TicketPrioridade.BAIXA
            );

            when(repo.nextNumeroTicket()).thenReturn(1L);
            when(repo.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

            var resultado = service.criar(dto, usuarioId);

            assertNotNull(resultado);
            assertEquals("Meu titulo", resultado.titulo());
            assertEquals(TicketStatus.ABERTO, resultado.status());
            assertEquals(TicketCategoria.DUVIDA, resultado.categoria());
            assertEquals(TicketPrioridade.BAIXA, resultado.prioridade());
        }

        @Test
        @DisplayName("Deve rejeitar ticket sem titulo")
        void deveRejeitarSemTitulo() {
            CreateTicketRequestDTO dto = new CreateTicketRequestDTO(
                    usuarioId, equipeId, null, "Plano",
                    "Equipe", null, "desc", null, null, null
            );

            assertThrows(ResponseStatusException.class, () -> service.criar(dto, usuarioId));
        }
    }

    @Nested
    @DisplayName("Mensagens")
    class Mensagens {

        @Test
        @DisplayName("Deve adicionar mensagem com sanitizacao XSS")
        void deveAdicionarMensagemSanitizada() {
            when(repo.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(messageRepo.save(any(TicketMessage.class))).thenAnswer(i -> i.getArgument(0));

            String textoXss = "<script>alert('xss')</script>Mensagem segura";
            CreateTicketMessageRequestDTO dto = new CreateTicketMessageRequestDTO(
                    TicketMessageAuthorType.USUARIO, textoXss
            );

            var resultado = service.adicionar(ticketId, usuarioId, dto);

            assertNotNull(resultado);
            assertTrue(resultado.texto().contains("Mensagem segura"));
            assertFalse(resultado.texto().contains("<script>"));
        }

        @Test
        @DisplayName("Deve rejeitar mensagem sem texto")
        void deveRejeitarMensagemVazia() {
            CreateTicketMessageRequestDTO dto = new CreateTicketMessageRequestDTO(
                    TicketMessageAuthorType.USUARIO, ""
            );

            assertThrows(ResponseStatusException.class,
                    () -> service.adicionar(ticketId, usuarioId, dto));
        }
    }

    @Nested
    @DisplayName("Finalizacao")
    class Finalizacao {

        @Test
        @DisplayName("Deve finalizar ticket com mensagem de resposta")
        void deveFinalizarTicket() {
            when(repo.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(messageRepo.save(any(TicketMessage.class))).thenAnswer(i -> i.getArgument(0));
            when(repo.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

            FinalizarTicketRequestDTO dto = new FinalizarTicketRequestDTO(
                    TicketMessageAuthorType.AGENTE,
                    "Problema resolvido!",
                    TicketStatus.RESOLVIDO
            );

            assertDoesNotThrow(() -> service.responderEFinalizar(ticketId, usuarioId, dto));
            assertEquals(TicketStatus.RESOLVIDO, ticket.getStatus());
            assertNotNull(ticket.getFechadoEm());
        }
    }

    @Nested
    @DisplayName("Leitura")
    class Leitura {

        @Test
        @DisplayName("Deve marcar ticket como lido")
        void deveMarcarComoLido() {
            when(repo.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(readRepo.findByTicketIdAndUsuarioId(ticketId, usuarioId))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> service.marcarComoLido(ticketId, usuarioId));

            verify(readRepo).save(any(TicketRead.class));
        }
    }

    @Nested
    @DisplayName("Status")
    class Status {

        @Test
        @DisplayName("Deve atualizar status")
        void deveAtualizarStatus() {
            when(repo.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(repo.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

            var resultado = service.atualizarStatus(ticketId, TicketStatus.EM_ANDAMENTO);

            assertEquals(TicketStatus.EM_ANDAMENTO, resultado.status());
        }

        @Test
        @DisplayName("Nao deve finalizar via atualizarStatus")
        void naoDeveFinalizarViaAtualizarStatus() {
            assertThrows(ResponseStatusException.class,
                    () -> service.atualizarStatus(ticketId, TicketStatus.FECHADO));
        }
    }
}
