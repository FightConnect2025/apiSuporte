package br.com.fightConnect.infrastructure.configurations;

import static org.springframework.security.config.Customizer.withDefaults;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.swagger.enabled:false}")
    private boolean swaggerEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    BearerTokenResolver bearerTokenResolver) throws Exception {

        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> {
                if (swaggerEnabled) {
                    auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                }
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tickets/exportar/**").authenticated()
                    .anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String perfil = jwt.getClaimAsString("perfil");
            if (perfil == null || perfil.isBlank()) return Collections.emptyList();
            String role = normalizarRole(perfil);
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    private String normalizarRole(String perfil) {
        if ("ADMINISTRADOR".equals(perfil)) return "ADMIN";
        if ("SUPERADMIN".equals(perfil)) return "SUPER_ADMIN";
        return perfil;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

        return (HttpServletRequest request) -> {
            String token = headerResolver.resolve(request);
            if (token != null && !token.isBlank()) return token;

            Cookie[] cookies = request.getCookies();
            if (cookies == null) return null;

            for (Cookie c : cookies) {
                if ("AUTH".equalsIgnoreCase(c.getName())) {
                    String v = c.getValue();
                    return (v == null || v.isBlank()) ? null : v;
                }
            }
            return null;
        };
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "https://*.fightconnect.com.br"
        ));

        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
