package br.com.fightConnect.infrastructure.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI helpdeskOpenAPI() {

        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API Suporte - Helpdesk")
                        .description("API de tickets (CRUD) + integrações com Usuários e Vistorias.")
                        .version("v1"))
                // ✅ diz que a API usa esse esquema por padrão
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // ✅ define o esquema Bearer JWT
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
