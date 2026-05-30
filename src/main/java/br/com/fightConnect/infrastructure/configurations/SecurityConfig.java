package br.com.fightConnect.infrastructure.configurations;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.fightConnect.infrastructure.security.HybridJwtDecoder;
import br.com.fightConnect.infrastructure.security.JwtClaimsAdapter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.swagger.enabled:false}")
    private boolean swaggerEnabled;

    @Autowired
    private InternalApiKeyFilter internalApiKeyFilter;

    @Bean
    public FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilterRegistration() {
        var reg = new FilterRegistrationBean<>(internalApiKeyFilter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/internal/**", "/api/internal/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("INTERNAL_SERVICE")
            )
            .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
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
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
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
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // 1. realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                for (String role : roles) {
                    if (!role.startsWith("default-roles-")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + JwtClaimsAdapter.normalizarRole(role)));
                    }
                }
            }

            // 2. perfil_usuario / perfil (claim customizada do FightConnect)
            String perfil = jwt.getClaimAsString("perfil_usuario");
            if (perfil == null || perfil.isBlank()) {
                perfil = jwt.getClaimAsString("perfil");
            }
            if (perfil != null && !perfil.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + JwtClaimsAdapter.normalizarRole(perfil)));
            }

            // 3. resource_access.*.roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                    if (entry.getValue() instanceof Map<?, ?> resource) {
                        Object roles = resource.get("roles");
                        if (roles instanceof List<?> list) {
                            for (Object role : list) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + JwtClaimsAdapter.normalizarRole(role.toString())));
                            }
                        }
                    }
                }
            }

            return authorities.stream().distinct().collect(Collectors.toList());
        });
        return converter;
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