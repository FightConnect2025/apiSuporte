package br.com.vibetex.domain.models.enums;

import java.util.Arrays;

public enum FotoTipo {
    PNG("image/png", ".png"),
    JPG("image/jpeg", ".jpg"),
    WEBP("image/webp", ".webp");

    private final String mime;
    private final String ext;

    FotoTipo(String mime, String ext) {
        this.mime = mime;
        this.ext = ext;
    }

    public String mime() { return mime; }
    public String ext() { return ext; }

    public static FotoTipo fromMime(String mime) {
        if (mime == null) return null;
        String m = mime.toLowerCase();
        return Arrays.stream(values())
                .filter(t -> t.mime.equals(m))
                .findFirst()
                .orElse(null);
    }
}
