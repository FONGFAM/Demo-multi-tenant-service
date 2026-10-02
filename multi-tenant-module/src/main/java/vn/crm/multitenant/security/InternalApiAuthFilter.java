package vn.crm.multitenant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;

@Slf4j
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private final String internalApiToken;

    public InternalApiAuthFilter(@Value("${internal.api.token:}") String internalApiToken) {
        this.internalApiToken = internalApiToken;
        if (this.internalApiToken == null || this.internalApiToken.trim().isEmpty()) {
            log.warn("CẢNH BÁO: internal.api.token chưa được cấu hình. Tất cả request vào /internal/** sẽ bị từ chối.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (internalApiToken == null || internalApiToken.trim().isEmpty()) {
            sendUnauthorized(response, "Internal API token is not configured on the server");
            return;
        }

        String providedToken = request.getHeader("X-Service-Token");
        if (providedToken == null || providedToken.trim().isEmpty()) {
            sendUnauthorized(response, "Missing X-Service-Token header");
            return;
        }

        boolean isMatch = MessageDigest.isEqual(
                providedToken.getBytes(),
                internalApiToken.getBytes()
        );

        if (!isMatch) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid X-Service-Token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }
}
