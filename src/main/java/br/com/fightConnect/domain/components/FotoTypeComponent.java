package br.com.fightConnect.domain.components;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.com.fightConnect.domain.models.enums.FotoTipo;

@Component
public class FotoTypeComponent {

    public FotoTipo resolveOrThrow(MultipartFile file) {
        String contentType = safeContentType(file);

        FotoTipo tipo = FotoTipo.fromMime(contentType);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de imagem não suportado: " + contentType);
        }
        return tipo;
    }

    public String safeContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    }
}
