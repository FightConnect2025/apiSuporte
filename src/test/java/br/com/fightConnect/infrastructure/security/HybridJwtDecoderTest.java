package br.com.fightConnect.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class HybridJwtDecoderTest {

    @Test
    void decode_tokenHs256Valido() throws Exception {
        String secret = "test-secret-key-1234567890123456";
        HybridJwtDecoder decoder = new HybridJwtDecoder(secret, "", "");

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(secret.getBytes()));

        var jwt = decoder.decode(signedJWT.serialize());
        assertEquals("test-user", jwt.getSubject());
    }

    @Test
    void decode_tokenMalformado_lancaExcecao() {
        HybridJwtDecoder decoder = new HybridJwtDecoder("secret", "", "");
        assertThrows(JwtException.class, () -> decoder.decode("token-invalido"));
    }

    @Test
    void construtor_secretVazio_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new HybridJwtDecoder("", "", ""));
    }

    @Test
    void decode_tokenRs256Valido_comDecoderKeycloakConfigurado() throws Exception {
        String secret = "test-secret-key-1234567890123456";
        HybridJwtDecoder decoder = new HybridJwtDecoder(secret, "", "");

        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("keycloak-user")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signedJWT.sign(new RSASSASigner(rsaKey));

        // Configura um NimbusJwtDecoder local com a chave pública RSA
        NimbusJwtDecoder keycloakDecoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();

        // Injeta o keycloakDecoder via reflection
        Field field = HybridJwtDecoder.class.getDeclaredField("keycloakDecoder");
        field.setAccessible(true);
        field.set(decoder, keycloakDecoder);

        var jwt = decoder.decode(signedJWT.serialize());
        assertEquals("keycloak-user", jwt.getSubject());
    }

    @Test
    void decode_tokenRs256Invalido_fallbackParaHs256() throws Exception {
        String secret = "test-secret-key-1234567890123456";
        HybridJwtDecoder decoder = new HybridJwtDecoder(secret, "", "");

        RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("fallback-user")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signedJWT.sign(new RSASSASigner(rsaKey));

        // Não injeta keycloakDecoder, então cai no fallback HS256 e falha
        assertThrows(JwtException.class, () -> decoder.decode(signedJWT.serialize()));
    }
}
