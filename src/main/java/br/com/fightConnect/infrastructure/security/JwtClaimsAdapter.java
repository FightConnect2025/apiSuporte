package br.com.fightConnect.infrastructure.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Adapter que extrai claims do JWT independente da fonte:
 * - JWT local (legado): claims customizados (usuarioId, perfil, equipeId)
 * - Keycloak (futuro): claims OIDC padrao (sub, realm_access.roles, etc)
 *
 * Uso: substituir extracao manual de claims em controllers/services.
 * Quando migrar para Keycloak, apenas esta classe precisa de ajuste.
 */
public final class JwtClaimsAdapter {

    private JwtClaimsAdapter() {}

    /**
     * Extrai usuarioId do token.
     * JWT local: claim "usuarioId"
     * Keycloak: claim customizado "fightconnect_usuario_id" (UUID interno)
     * Para tokens legados que nao possuem usuarioId, usa "sub" como fallback
     * apenas quando o token NAO for do Keycloak.
     */
    public static UUID usuarioId(Jwt jwt) {
        if (jwt == null) return null;

        // JWT local (legado) - claim primaria
        UUID usuarioId = claimAsUuid(jwt, "usuarioId");
        if (usuarioId != null) return usuarioId;

        // Keycloak: claim customizada do mapper (UUID interno do FightConnect)
        UUID fcUsuarioId = claimAsUuid(jwt, "fightconnect_usuario_id");
        if (fcUsuarioId != null) return fcUsuarioId;

        // Fallback para tokens legados antigos que so tem "sub"
        if (!isKeycloakToken(jwt)) {
            return uuidFromString(jwt.getSubject());
        }

        return null;
    }

    /**
     * Extrai perfil/role do token.
     * JWT local: claim "perfil"
     * Keycloak: realm_access.roles ou resource_access
     *
     * Retorna a primeira role encontrada. Para autorizacao completa
     * (todas as roles), use o JwtAuthenticationConverter.
     */
    public static String perfil(Jwt jwt) {
        if (jwt == null) return null;

        // JWT local (legado)
        String perfil = jwt.getClaimAsString("perfil");
        if (StringUtils.hasText(perfil)) {
            return normalizarRole(perfil);
        }

        // Keycloak: realm_access.roles
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

    /**
     * Extrai equipeId do token.
     * JWT local: claim "equipeId"
     * Keycloak: claims customizadas "fightconnect_equipe_id" ou "equipe_id"
     */
    public static UUID equipeId(Jwt jwt) {
        if (jwt == null) return null;

        UUID equipeId = claimAsUuid(jwt, "equipeId");
        if (equipeId != null) return equipeId;

        equipeId = claimAsUuid(jwt, "fightconnect_equipe_id");
        if (equipeId != null) return equipeId;

        return claimAsUuid(jwt, "equipe_id");
    }

    /**
     * Extrai nome do usuario.
     * JWT local: claim "nome"
     * Keycloak: claim "name" ou "preferred_username"
     */
    public static String nome(Jwt jwt) {
        if (jwt == null) return null;

        String nome = jwt.getClaimAsString("nome");
        if (StringUtils.hasText(nome)) return nome;

        nome = jwt.getClaimAsString("name");
        if (StringUtils.hasText(nome)) return nome;

        return jwt.getClaimAsString("preferred_username");
    }

    /**
     * Extrai email do token.
     * JWT local: claim "email"
     * Keycloak: claim "email"
     */
    public static String email(Jwt jwt) {
        if (jwt == null) return null;

        String email = jwt.getClaimAsString("email");
        if (StringUtils.hasText(email)) return email;

        return null;
    }

    /**
     * Extrai professorId do token.
     * JWT local: claim "professorId"
     * Keycloak: claims customizadas "fightconnect_professor_id" ou "professor_id"
     */
    public static UUID professorId(Jwt jwt) {
        if (jwt == null) return null;

        UUID professorId = claimAsUuid(jwt, "professorId");
        if (professorId != null) return professorId;

        professorId = claimAsUuid(jwt, "fightconnect_professor_id");
        if (professorId != null) return professorId;

        return claimAsUuid(jwt, "professor_id");
    }

    /**
     * Extrai unidadeId do token.
     * JWT local: claim "unidadeId"
     * Keycloak: claims customizadas "fightconnect_unidade_id" ou "unidade_id"
     */
    public static UUID unidadeId(Jwt jwt) {
        if (jwt == null) return null;

        UUID unidadeId = claimAsUuid(jwt, "unidadeId");
        if (unidadeId != null) return unidadeId;

        unidadeId = claimAsUuid(jwt, "fightconnect_unidade_id");
        if (unidadeId != null) return unidadeId;

        return claimAsUuid(jwt, "unidade_id");
    }

    /**
     * Normaliza roles para compatibilidade.
     * Alias historicos -> canonicas.
     */
    public static String normalizarRole(String perfil) {
        if (!StringUtils.hasText(perfil)) return perfil;
        String p = perfil.trim().toUpperCase();
        return switch (p) {
            case "ADMINISTRADOR" -> "ADMIN";
            case "SUPERADMIN" -> "SUPER_ADMIN";
            default -> p;
        };
    }

    /**
     * Verifica se o token vem do Keycloak (issuer contem "keycloak" ou
     * possui a claim realm_access, tipica de tokens Keycloak).
     */
    public static boolean isKeycloakToken(Jwt jwt) {
        if (jwt == null) return false;
        String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (iss != null && iss.toLowerCase().contains("keycloak")) {
            return true;
        }
        // Tokens Keycloak tem realm_access; tokens legados nao tem
        Object realmAccess = jwt.getClaim("realm_access");
        return realmAccess != null;
    }

    private static UUID claimAsUuid(Jwt jwt, String claim) {
        return uuidFromString(jwt.getClaimAsString(claim));
    }

    private static UUID uuidFromString(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
