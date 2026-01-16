package br.com.vibetex.domain.components;

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

import br.com.vibetex.domain.contracts.services.FileStorageService;
import br.com.vibetex.domain.models.entities.TicketFoto;
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

    @Value("${app.storage.public-host:https://cedae.homologacao.vibetex.com.br}")
    private String publicHost;

    @Transactional
    public void salvarBase64(UUID ticketId, String nomeEmpresa, List<String> fotosBase64) {
        if (fotosBase64 == null || fotosBase64.isEmpty()) return;

        String empresaSlug = slugify(nomeEmpresa);

        for (String base64 : fotosBase64) {
            if (base64 == null || base64.isBlank()) continue;

            byte[] bytes = decodeBase64(base64);

            if (bytes.length > maxBytes) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem maior que 5MB");
            }

            FotoInfo info = detect(bytes); // PNG/JPG/WEBP

            UUID fotoId = UUID.randomUUID();
            String fileName = fotoId + info.ext;

            var stored = storage.save(bytes, info.mime, fileName, empresaSlug);

            String urlAbsoluta = normalizeHost(publicHost) + stored.url();

            var foto = TicketFoto.builder()
                    .id(fotoId)              // ✅ pode setar ID
                    .ticketId(ticketId)
                    .fileName(fileName)
                    .contentType(info.mime)
                    .sizeBytes((long) bytes.length)
                    .storageKey(stored.storageKey())
                    .url(urlAbsoluta)
                    .build();

            // ✅ FORÇA INSERT (persist) em vez de merge
            em.persist(foto);
        }
    }

    // ---------------- helpers ----------------

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
        if (host == null || host.isBlank()) return "";
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) return "empresa-sem-nome";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String slug = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");

        if (slug.isBlank()) slug = "empresa-sem-nome";
        if (slug.length() > 60) slug = slug.substring(0, 60);

        return slug;
    }

    private record FotoInfo(String mime, String ext) {}
}
