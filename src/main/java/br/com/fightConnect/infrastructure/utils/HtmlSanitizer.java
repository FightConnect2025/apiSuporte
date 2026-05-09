package br.com.fightConnect.infrastructure.utils;

import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    public String sanitize(String input) {
        if (input == null || input.isBlank()) return input;
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    public String sanitizeKeepNewlines(String input) {
        if (input == null || input.isBlank()) return input;
        String sanitized = sanitize(input);
        return sanitized.replace("&#x2F;", "/");
    }
}
