package br.com.fightConnect.domain.components;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class EquipeFolderComponent {

    public String slugify(String input) {
        if (input == null || input.isBlank()) return "equipe-sem-nome";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String slug = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");

        if (slug.isBlank()) slug = "equipe-sem-nome";
        if (slug.length() > 60) slug = slug.substring(0, 60);

        return slug;
    }
}
