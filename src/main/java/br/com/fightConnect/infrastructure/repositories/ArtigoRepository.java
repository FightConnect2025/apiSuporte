package br.com.fightConnect.infrastructure.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fightConnect.domain.models.entities.Artigo;

public interface ArtigoRepository extends JpaRepository<Artigo, UUID> {
    List<Artigo> findByPublicadoTrueOrderByOrdemAsc();
    List<Artigo> findByEquipeIdAndPublicadoTrueOrderByOrdemAsc(UUID equipeId);
    List<Artigo> findByTituloContainingIgnoreCaseOrTagsContainingIgnoreCase(String titulo, String tags);
}
