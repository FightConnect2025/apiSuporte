package br.com.fightConnect.infrastructure.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Decoder híbrido que suporta tanto JWT legado (HS256) quanto Keycloak (RS256).
 * Se o token parecer um JWT RS256 (header com "RS256" ou "kid") e houver um
 * decoder Keycloak configurado, tenta primeiro por esse caminho; caso falhe,
 * faz fallback para o decoder legado (HS256). Caso contrario, usa o legado direto.
 */
@Component
public class HybridJwtDecoder implements JwtDecoder {

    private final JwtDecoder legacyDecoder;
    private final JwtDecoder keycloakDecoder;

    public HybridJwtDecoder(
            @Value("${jwt.secret}") String secret,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {

        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("jwt.secret obrigatorio");
        }

        // Decoder legado (HS256)
        SecretKey key = new SecretKeySpec(secret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.legacyDecoder = NimbusJwtDecoder.withSecretKey(key).build();

        // Decoder Keycloak (RS256) - só se configurado
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            this.keycloakDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else if (issuerUri != null && !issuerUri.isBlank()) {
            String jwkUri = issuerUri + "/protocol/openid-connect/certs";
            this.keycloakDecoder = NimbusJwtDecoder.withJwkSetUri(jwkUri).build();
        } else {
            this.keycloakDecoder = null;
        }
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        // Se parece Keycloak e tem decoder configurado, tenta primeiro
        if (pareceKeycloakToken(token) && keycloakDecoder != null) {
            try {
                return keycloakDecoder.decode(token);
            } catch (JwtException e) {
                // Se falhar no Keycloak, tenta legado como fallback
                return legacyDecoder.decode(token);
            }
        }
        // Senão, usa legado direto
        return legacyDecoder.decode(token);
    }

    /**
     * Verifica se o token parece ser um JWT do Keycloak (RS256 tem header maior com kid).
     */
    private boolean pareceKeycloakToken(String token) {
        if (token == null || token.isBlank()) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;
        try {
            String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            return header.contains("RS256") || header.contains("kid");
        } catch (Exception e) {
            return false;
        }
    }
}
