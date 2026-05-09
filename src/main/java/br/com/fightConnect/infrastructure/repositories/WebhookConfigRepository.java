package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.WebhookConfig;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {
    List<WebhookConfig> findByEquipeIdAndAtivoTrue(UUID equipeId);
}
