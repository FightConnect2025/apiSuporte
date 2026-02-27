package br.com.fightConnect.infrastructure.clients.apiAuth;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import br.com.fightConnect.infrastructure.clients.apiAuth.dtos.AlunoResponseDtoClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.dtos.ProfessorResponseDtoClient;
import br.com.fightConnect.infrastructure.clients.apiAuth.dtos.UsuarioAdministradorResponseDtoClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class ApiAuthClient {

    @Bean
    public WebClient apiAuthWebClient(
            @Value("${api.auth.base-url}") String baseUrl,
            @Value("${api.auth.timeout-ms:5000}") int timeoutMs
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // =========================================================
    // ✅ ALUNO
    // =========================================================
    @Component
    public static class ApiAuthAlunoClient {
        private final WebClient webClient;

        public ApiAuthAlunoClient(WebClient apiAuthWebClient) {
            this.webClient = apiAuthWebClient;
        }

        public AlunoResponseDtoClient buscarPorId(UUID id) {
            if (id == null) return null;

            try {
                return webClient.get()
                        .uri("/api/aut/alunos/{id}", id)
                        .retrieve()
                        .bodyToMono(AlunoResponseDtoClient.class)
                        .block();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Falha ao buscar aluno na API Auth: " + id, e);
            }
        }
    }

    // =========================================================
    // ✅ PROFESSOR
    // =========================================================
    @Component
    public static class ApiAuthProfessorClient {
        private final WebClient webClient;

        public ApiAuthProfessorClient(WebClient apiAuthWebClient) {
            this.webClient = apiAuthWebClient;
        }

        public ProfessorResponseDtoClient buscarPorId(UUID id) {
            if (id == null) return null;

            try {
                return webClient.get()
                        .uri("/api/aut/professores/{id}", id)
                        .retrieve()
                        .bodyToMono(ProfessorResponseDtoClient.class)
                        .block();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Falha ao buscar professor na API Auth: " + id, e);
            }
        }
    }

    // =========================================================
    // ✅ ADMIN
    // =========================================================
    @Component
    public static class ApiAuthAdminClient {
        private final WebClient webClient;

        public ApiAuthAdminClient(WebClient apiAuthWebClient) {
            this.webClient = apiAuthWebClient;
        }

        public UsuarioAdministradorResponseDtoClient buscarPorId(UUID id) {
            if (id == null) return null;

            try {
                return webClient.get()
                        .uri("/api/aut/usuarios-administradores/{id}", id)
                        .retrieve()
                        .bodyToMono(UsuarioAdministradorResponseDtoClient.class)
                        .block();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Falha ao buscar admin na API Auth: " + id, e);
            }
        }
    }
}
