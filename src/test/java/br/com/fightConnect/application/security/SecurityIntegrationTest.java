package br.com.fightConnect.application.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fightConnect.infrastructure.configurations.TestRabbitConfig;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRabbitConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-tests-only";

    @Test
    void endpointProtegido_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarTicket_semToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalApiKey_emRotaExterna_retorna401() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .header("X-Internal-Api-Key", "qualquer-chave"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegido_tokenLegadoValido_retorna200() throws Exception {
        String token = gerarTokenHs256(
                Map.of(
                        "usuarioId", UUID.randomUUID().toString(),
                        "equipeId", UUID.randomUUID().toString()
                ),
                TEST_SECRET
        );

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegido_tokenLegadoComRoleUser_retorna200() throws Exception {
        String token = gerarTokenHs256(
                Map.of(
                        "usuarioId", UUID.randomUUID().toString(),
                        "equipeId", UUID.randomUUID().toString(),
                        "perfil", "USER"
                ),
                TEST_SECRET
        );

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegido_tokenExpirado_retorna401() throws Exception {
        // Expira 120s no passado para ficar fora da clock skew padrao (60s) do NimbusJwtDecoder
        String token = gerarTokenHs256(
                Map.of("usuarioId", UUID.randomUUID().toString()),
                TEST_SECRET,
                Date.from(Instant.now().minusSeconds(3600)),
                Date.from(Instant.now().minusSeconds(120))
        );

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String gerarTokenHs256(Map<String, Object> claims, String secret) throws Exception {
        return gerarTokenHs256(claims, secret, Date.from(Instant.now()), Date.from(Instant.now().plusSeconds(3600)));
    }

    private String gerarTokenHs256(Map<String, Object> claims, String secret, Date issuedAt, Date expiration) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issueTime(issuedAt)
                .expirationTime(expiration);

        claims.forEach(builder::claim);

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
        signedJWT.sign(new MACSigner(secret.getBytes()));
        return signedJWT.serialize();
    }
}
