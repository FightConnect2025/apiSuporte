package br.com.fightConnect.domain.components;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketFotoPorEquipeComponent {

    private final TicketFotoComponent ticketFotoComponent;
    private final EquipeFolderComponent equipeFolderComponent;

    /**
     * Salva as fotos do ticket organizando em:
     * /tickets/<slug-equipe>-<equipeId>/
     */
    @Transactional
    public void salvar(UUID ticketId, UUID equipeId, String nomeEquipe, List<String> fotosBase64) {
        if (fotosBase64 == null || fotosBase64.isEmpty()) return;

        String slug = equipeFolderComponent.slugify(nomeEquipe);

        // pasta final (fica dentro do base-path que já é .../tickets)
        String folder = (equipeId == null)
                ? slug
                : slug + "-" + equipeId;

        // ✅ TicketFotoComponent já vai slugify de novo, mas beleza.
        // Para NÃO slugify duas vezes, a gente passa "folder" já pronto
        ticketFotoComponent.salvarBase64(ticketId, folder, fotosBase64);
    }
}
