package br.com.fightConnect.domain.components;

import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.fightConnect.domain.contracts.services.FileStorageService;
import br.com.fightConnect.domain.models.entities.TicketFoto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketFotoComponent {

    private final FileStorageService storage;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.foto.max-bytes:5242880}") // 5MB
    private long maxBytes;

    /**
     * Ex: https://app.fightconnect.com.br
     * Se vazio, salva URL relativa mesmo (ex: /tickets/pasta/arquivo.png)
     */
    @Value("${app.storage.public-host:}")
    private String publicHost;

    // =========================================================
    // ✅ OVERLOAD 1: você passa a pasta pronta (NÃO slugify aqui)
    // =========================================================
    @Transactional
    public void salvarBase64(UUID ticketId, String folder, List<String> fotosBase64) {
        if (fotosBase64 == null || fotosBase64.isEmpty()) return;
        if (ticketId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId é obrigatório");

        String safeFolder = sanitizeFolder(folder);

        for (String base64 : fotosBase64) {
            if (base64 == null || base64.isBlank()) continue;

            byte[] bytes = decodeBase64(base64);

            if (bytes.length > maxBytes) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem maior que 5MB");
            }

            FotoInfo info = detect(bytes); // PNG/JPG/WEBP

            UUID fotoId = UUID.randomUUID();
            String fileName = fotoId + info.ext;

            var stored = storage.save(bytes, info.mime, fileName, safeFolder);

            // stored.url() vem tipo: /tickets/<folder>/<file>
            String url = absolutizeIfPossible(stored.url());

            var foto = TicketFoto.builder()
                    .id(fotoId)
                    .ticketId(ticketId)
                    .fileName(fileName)
                    .contentType(info.mime)
                    .sizeBytes((long) bytes.length)
                    .storageKey(stored.storageKey())
                    .url(url)
                    .build();

            em.persist(foto);
        }
    }

    // =========================================================
    // ✅ OVERLOAD 2: monta a pasta por equipe (slug + equipeId)
    // =========================================================
    @Transactional
    public void salvarBase64(UUID ticketId, UUID equipeId, String nomeEquipe, List<String> fotosBase64) {
        String slug = slugify(nomeEquipe);

        String folder = (equipeId == null)
                ? slug
                : (slug + "-" + equipeId);

        salvarBase64(ticketId, folder, fotosBase64);
    }

    // ---------------- helpers ----------------

    private String absolutizeIfPossible(String relativeUrl) {
        String host = normalizeHost(publicHost);
        if (host.isBlank()) return relativeUrl; // mantém relativo
        if (relativeUrl == null) return host;
        return host + relativeUrl;
    }

    private byte[] decodeBase64(String base64) {
        try {
            int comma = base64.indexOf(',');
            String raw = (comma >= 0) ? base64.substring(comma + 1) : base64;
            return Base64.getDecoder().decode(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base64 inválido");
        }
    }

    private FotoInfo detect(byte[] b) {
        // PNG
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return new FotoInfo("image/png", ".png");
        }

        // JPEG
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return new FotoInfo("image/jpeg", ".jpg");
        }

        // WEBP
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return new FotoInfo("image/webp", ".webp");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo não suportado (PNG/JPG/WEBP)");
    }

    private String normalizeHost(String host) {
        if (host == null) return "";
        String h = host.trim();
        if (h.isBlank()) return "";
        return h.endsWith("/") ? h.substring(0, h.length() - 1) : h;
    }

    private String slugify(String input) {
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

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) return "equipe-sem-nome";
        String f = folder.replace("\\", "/")
                .replaceAll("\\.\\.", "")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
        // só pra garantir caracteres ok
        f = f.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]", "-");
        f = f.replaceAll("-+", "-");
        return f.isBlank() ? "equipe-sem-nome" : f;
    }

    private record FotoInfo(String mime, String ext) {}
}
