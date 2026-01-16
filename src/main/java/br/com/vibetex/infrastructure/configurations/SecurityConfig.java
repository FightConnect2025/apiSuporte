package br.com.vibetex.infrastructure.configurations;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ✅ HABILITA CORS NO SECURITY (senão preflight morre antes)
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // ✅ PRE-FLIGHT (CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // libera swagger
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // se você tiver endpoint público de imagem, libera aqui também (opcional)
                // .requestMatchers(HttpMethod.GET, "/images/**").permitAll()

                // resto precisa de JWT
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }

    /**
     * ✅ Fonte de configuração CORS usada pelo Spring Security
     * (isso é o que faz o OPTIONS responder com Access-Control-Allow-Origin)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Angular dev
        config.setAllowedOrigins(List.of("http://localhost:4200"));

        // Se você preferir aceitar variações, use allowedOriginPatterns:
        // config.setAllowedOriginPatterns(List.of("http://localhost:4200"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience
    ) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }
}
