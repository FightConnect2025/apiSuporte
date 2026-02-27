package br.com.fightConnect.infrastructure.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.fightConnect.domain.contracts.services.FileStorageService;
import br.com.fightConnect.domain.contracts.storage.StoredFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.base-path:./uploads}")
    private String basePath;

    @Value("${app.storage.public-base-url:/images}")
    private String publicBaseUrl;

    @Override
    public StoredFile save(byte[] bytes, String contentType, String fileName, String folder) {
        try {
            String cleanFolder = sanitizeFolder(folder);
            String cleanFile = sanitizeFileName(fileName);

            String fileKey = cleanFolder + "/" + cleanFile;

            Path base = Paths.get(basePath).toAbsolutePath().normalize();
            Path target = base.resolve(fileKey).normalize();

            // segurança: impedir ../../
            if (!target.startsWith(base)) {
                throw new RuntimeException("Caminho inválido para salvar arquivo");
            }

            Files.createDirectories(target.getParent());

            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            String url = normalizeUrlBase(publicBaseUrl) + "/" + fileKey.replace("\\", "/");

            // storageKey pode ser o caminho relativo (fileKey) ou absoluto.
            // Mantendo o teu padrão: relativo.
            return new StoredFile(fileKey, url);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao salvar arquivo em " + basePath, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path base = Paths.get(basePath).toAbsolutePath().normalize();
            Path target = base.resolve(storageKey).normalize();
            if (!target.startsWith(base)) return;
            Files.deleteIfExists(target);
        } catch (Exception ignored) { }
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) return "default";

        String f = folder.replace("\\", "/")
                .replaceAll("\\.\\.", "")
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");

        // remove acentos
        f = Normalizer.normalize(f, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // troca tudo que não for seguro por _
        f = f.toLowerCase().replaceAll("[^a-z0-9/_-]", "_");

        // limpa múltiplos _
        f = f.replaceAll("_+", "_");

        return f.isBlank() ? "default" : f;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "file";
        String clean = name.replace("\\", "/");
        if (clean.contains("/")) clean = clean.substring(clean.lastIndexOf('/') + 1);
        return clean.replaceAll("[^a-zA-Z0-9\\-_.]", "_");
    }

    private String normalizeUrlBase(String base) {
        if (base == null || base.isBlank()) return "/images";
        String b = base.trim();
        if (!b.startsWith("/")) b = "/" + b;
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }
}
