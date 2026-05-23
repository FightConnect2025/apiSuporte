package br.com.fightConnect.infrastructure.configurations;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalApiKeyValidator {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @PostConstruct
    public void validate() {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("internal.api.key must be configured. Application cannot start without it.");
        }
    }
}