package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.RespostaRapida;

public interface RespostaRapidaRepository extends JpaRepository<RespostaRapida, UUID> {
    List<RespostaRapida> findByEquipeIdOrderByTituloAsc(UUID equipeId);
}
