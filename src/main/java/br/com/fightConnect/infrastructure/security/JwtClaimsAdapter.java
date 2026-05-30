package br.com.fightConnect.infrastructure.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Adapter que extrai claims do JWT independente da fonte:
 * - JWT local (legado): claims customizados (usuarioId, perfil, equipeId)
 * - Keycloak (futuro): claims OIDC padrao (sub, realm_access.roles, etc)
 */
public final class JwtClaimsAdapter {

    private JwtClaimsAdapter() {}

    public static UUID usuarioId(Jwt jwt) {
        if (jwt == null) return null;
        String usuarioId = jwt.getClaimAsString("usuarioId");
        if (StringUtils.hasText(usuarioId)) {
            try {
                return UUID.fromString(usuarioId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        String sub = jwt.getSubject();
        if (StringUtils.hasText(sub)) {
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public static String perfil(Jwt jwt) {
        if (jwt == null) return null;
        String perfil = jwt.getClaimAsString("perfil");
        if (StringUtils.hasText(perfil)) return normalizarRole(perfil);

        @SuppressWarnings("unchecked")
        var realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof List<?> list && !list.isEmpty()) {
                return normalizarRole(list.get(0).toString());
            }
        }
        return null;
    }

    public static UUID equipeId(Jwt jwt) {
        if (jwt == null) return null;
        String equipeId = jwt.getClaimAsString("equipeId");
        if (StringUtils.hasText(equipeId)) return UUID.fromString(equipeId);
        return null;
    }

    public static String nome(Jwt jwt) {
        if (jwt == null) return null;
        String nome = jwt.getClaimAsString("nome");
        if (StringUtils.hasText(nome)) return nome;
        nome = jwt.getClaimAsString("name");
        if (StringUtils.hasText(nome)) return nome;
        return jwt.getClaimAsString("preferred_username");
    }

    public static String email(Jwt jwt) {
        if (jwt == null) return null;
        String email = jwt.getClaimAsString("email");
        if (StringUtils.hasText(email)) return email;
        return null;
    }

    public static String normalizarRole(String perfil) {
        if (!StringUtils.hasText(perfil)) return perfil;
        String p = perfil.trim().toUpperCase();
        return switch (p) {
            case "ADMINISTRADOR" -> "ADMIN";
            case "SUPERADMIN" -> "SUPER_ADMIN";
            default -> p;
        };
    }

    public static boolean isKeycloakToken(Jwt jwt) {
        if (jwt == null) return false;
        var iss = jwt.getIssuer();
        return iss != null && iss.toString().toLowerCase().contains("keycloak");
    }
}
