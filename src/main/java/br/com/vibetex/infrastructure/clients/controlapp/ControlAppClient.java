package br.com.vibetex.infrastructure.clients.controlapp;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class ControlAppClient {

    @Bean
    public WebClient controlAppWebClient(
            @Value("${controlapp.base-url}") String baseUrl,
            @Value("${controlapp.timeout-ms:5000}") int timeoutMs
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Component
    public static class ControlAppUsuarioClient { // 👈 static
        private final WebClient webClient;

        public ControlAppUsuarioClient(WebClient controlAppWebClient) {
            this.webClient = controlAppWebClient;
        }

        public ControlAppUsuarioDTO getUsuarioById(UUID usuarioId) {
            return webClient.get()
                    .uri("/usuario/{usuarioId}", usuarioId)
                    .retrieve()
                    .bodyToMono(ControlAppUsuarioDTO.class)
                    .block();
        }
    }
}
