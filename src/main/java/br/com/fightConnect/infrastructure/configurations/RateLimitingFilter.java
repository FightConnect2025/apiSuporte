package br.com.fightConnect.infrastructure.configurations;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.fightConnect.infrastructure.security.JwtClaimsAdapter;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 30;
    private static final long WINDOW_MS = 60_000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String method = req.getMethod();

        if (isRateLimited(path, method)) {
            String key = resolveKey(req);

            long now = System.currentTimeMillis();
            Window window = windows.computeIfAbsent(key, k -> new Window(now));

            synchronized (window) {
                if (now - window.start > WINDOW_MS) {
                    window.start = now;
                    window.count.set(0);
                }
                if (window.count.incrementAndGet() > MAX_REQUESTS) {
                    res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    res.setContentType("application/json");
                    res.getWriter().write("{\"mensagem\":\"Muitas requisicoes. Tente novamente em 1 minuto.\"}");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String path, String method) {
        if (path == null) return false;
        if (path.contains("/api/tickets") && method.equalsIgnoreCase("POST")) return true;
        if (path.contains("/api/tickets") && path.contains("/mensagens") && method.equalsIgnoreCase("POST")) return true;
        if (path.contains("/api/tickets") && path.contains("/finalizar") && method.equalsIgnoreCase("POST")) return true;
        return false;
    }

    private String resolveKey(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            var usuarioId = JwtClaimsAdapter.usuarioId(jwt);
            if (usuarioId != null) return ip + "|" + usuarioId.toString();
        }
        return ip;
    }

    private static class Window {
        long start;
        AtomicInteger count = new AtomicInteger(0);
        Window(long start) { this.start = start; }
    }
}
