package br.com.fightConnect.domain.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fightConnect.domain.models.entities.TicketSla;
import br.com.fightConnect.infrastructure.repositories.TicketSlaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketSlaService {

    private final TicketSlaRepository slaRepo;

    @Transactional(readOnly = true)
    public TicketSla buscarSlaAtivo(UUID equipeId, UUID planoId) {
        // Tenta SLA especifico da equipe + plano
        if (planoId != null) {
            var especifico = slaRepo.findByEquipeIdAndPlanoIdAndAtivoTrue(equipeId, planoId);
            if (especifico.isPresent()) return especifico.get();
        }
        // Fallback: SLA da equipe sem plano definido
        return slaRepo.findByEquipeIdAndPlanoIdIsNullAndAtivoTrue(equipeId)
                .orElseGet(() -> TicketSla.builder()
                        .tempoPrimeiraRespostaMin(60)
                        .tempoResolucaoMin(240)
                        .build());
    }
}
