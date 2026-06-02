package br.com.fightConnect.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        // Instancia o converter como e definido no SecurityConfig
        converter = new br.com.fightConnect.infrastructure.configurations.SecurityConfig().jwtAuthenticationConverter();
    }

    @Test
    void converteRealmAccessRoles_paraAuthorities() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
                .build();

        var auth = converter.convert(jwt);

        assertNotNull(auth);
        var authorities = auth.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void convertePerfilLegado_paraAuthority() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("perfil", "ADMINISTRADOR")
                .build();

        var auth = converter.convert(jwt);

        assertNotNull(auth);
        var authorities = auth.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void converteResourceAccessRoles_paraAuthorities() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("resource_access", Map.of(
                        "client-a", Map.of("roles", List.of("GESTOR")),
                        "client-b", Map.of("roles", List.of("VIEWER"))
                ))
                .build();

        var auth = converter.convert(jwt);

        assertNotNull(auth);
        var authorities = auth.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_GESTOR")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_VIEWER")));
    }

    @Test
    void ignoraDefaultRolesKeycloak() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("default-roles-fightconnect", "ADMIN")))
                .build();

        var auth = converter.convert(jwt);

        assertNotNull(auth);
        var authorities = auth.getAuthorities();
        assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_DEFAULT-ROLES-FIGHTCONNECT")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void tokenSemRoles_retornaAuthoritiesVazias() {
        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("usuarioId", UUID.randomUUID().toString())
                .build();

        var auth = converter.convert(jwt);

        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());
    }
}
