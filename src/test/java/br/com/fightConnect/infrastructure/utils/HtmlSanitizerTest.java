package br.com.fightConnect.infrastructure.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    private HtmlSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new HtmlSanitizer();
    }

    @Test
    @DisplayName("Deve escapar tags HTML")
    void deveEscapeHtml() {
        String result = sanitizer.sanitize("<script>alert('xss')</script>");
        assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;&#x2F;script&gt;", result);
    }

    @Test
    @DisplayName("Deve manter texto normal")
    void deveManterTextoNormal() {
        String result = sanitizer.sanitize("Mensagem normal sem HTML");
        assertEquals("Mensagem normal sem HTML", result);
    }

    @Test
    @DisplayName("Deve retornar null para null")
    void deveRetornarNull() {
        assertNull(sanitizer.sanitize(null));
    }

    @Test
    @DisplayName("Deve escapar aspas")
    void deveEscapeAspas() {
        String result = sanitizer.sanitize("texto com \"aspas\"");
        assertFalse(result.contains("\""));
        assertTrue(result.contains("&quot;"));
    }
}
