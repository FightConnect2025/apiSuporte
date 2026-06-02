package br.com.fightConnect.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtClaimsAdapterTest {

    @Test
    void usuarioId_legado() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("usuarioId", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.usuarioId(jwt));
    }

    @Test
    void usuarioId_fightconnect_usuario_id() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("fightconnect_usuario_id", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.usuarioId(jwt));
    }

    @Test
    void usuarioId_legado_subFallback() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .subject(id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.usuarioId(jwt));
    }

    @Test
    void usuarioId_keycloak_semClaimCustomizada_retornaNull() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .issuer("https://keycloak.fightconnect.com.br/realms/fc")
                .subject(UUID.randomUUID().toString())
                .build();
        assertNull(JwtClaimsAdapter.usuarioId(jwt));
    }

    @Test
    void usuarioId_sub_invalido_retornaNull() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .subject("auth0|123")
                .build();
        assertNull(JwtClaimsAdapter.usuarioId(jwt));
    }

    @Test
    void usuarioId_jwtNull_retornaNull() {
        assertNull(JwtClaimsAdapter.usuarioId(null));
    }

    @Test
    void perfil_legado() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("perfil", "ADMINISTRADOR")
                .build();
        assertEquals("ADMIN", JwtClaimsAdapter.perfil(jwt));
    }

    @Test
    void perfil_keycloak_realmAccess() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", java.util.List.of("ADMIN", "USER")))
                .build();
        assertEquals("ADMIN", JwtClaimsAdapter.perfil(jwt));
    }

    @Test
    void equipeId_legado() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("equipeId", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.equipeId(jwt));
    }

    @Test
    void equipeId_fightconnect_equipe_id() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("fightconnect_equipe_id", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.equipeId(jwt));
    }

    @Test
    void equipeId_equipe_id() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("equipe_id", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.equipeId(jwt));
    }

    @Test
    void equipeId_null() {
        assertNull(JwtClaimsAdapter.equipeId(null));
    }

    @Test
    void nome_legado() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("nome", "Joao")
                .build();
        assertEquals("Joao", JwtClaimsAdapter.nome(jwt));
    }

    @Test
    void nome_keycloak_preferred_username() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("preferred_username", "joao.silva")
                .build();
        assertEquals("joao.silva", JwtClaimsAdapter.nome(jwt));
    }

    @Test
    void email_legado() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("email", "joao@teste.com")
                .build();
        assertEquals("joao@teste.com", JwtClaimsAdapter.email(jwt));
    }

    @Test
    void email_semClaim_naoFallbackParaSub() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .subject("auth0|123")
                .build();
        assertNull(JwtClaimsAdapter.email(jwt));
    }

    @Test
    void professorId_legado() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("professorId", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.professorId(jwt));
    }

    @Test
    void professorId_fightconnect_professor_id() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("fightconnect_professor_id", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.professorId(jwt));
    }

    @Test
    void unidadeId_legado() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("unidadeId", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.unidadeId(jwt));
    }

    @Test
    void unidadeId_fightconnect_unidade_id() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("fightconnect_unidade_id", id.toString())
                .build();
        assertEquals(id, JwtClaimsAdapter.unidadeId(jwt));
    }

    @Test
    void isKeycloakToken_true_por_issuer() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .issuer("https://keycloak.fightconnect.com.br/realms/fc")
                .build();
        assertTrue(JwtClaimsAdapter.isKeycloakToken(jwt));
    }

    @Test
    void isKeycloakToken_true_por_realm_access() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", java.util.List.of("USER")))
                .build();
        assertTrue(JwtClaimsAdapter.isKeycloakToken(jwt));
    }

    @Test
    void isKeycloakToken_issuerNull_semRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("dummy", "value")
                .build();
        assertFalse(JwtClaimsAdapter.isKeycloakToken(jwt));
    }

    @Test
    void isKeycloakToken_jwtNull() {
        assertFalse(JwtClaimsAdapter.isKeycloakToken(null));
    }

    @Test
    void normalizarRole_administrador() {
        assertEquals("ADMIN", JwtClaimsAdapter.normalizarRole("ADMINISTRADOR"));
    }

    @Test
    void normalizarRole_superadmin() {
        assertEquals("SUPER_ADMIN", JwtClaimsAdapter.normalizarRole("SUPERADMIN"));
    }
}
