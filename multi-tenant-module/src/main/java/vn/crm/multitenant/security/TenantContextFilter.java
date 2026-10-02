package vn.crm.multitenant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.crm.multitenant.context.TenantContext;
import vn.crm.multitenant.context.TenantSchemaResolver;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantSchemaResolver tenantSchemaResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip for internal endpoints
        if (request.getRequestURI().startsWith("/internal/") || request.getRequestURI().startsWith("/api/tenants/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId == null || tenantId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing X-Tenant-ID header\"}");
            return;
        }

        try {
            // Validate via resolver - throws exception if not active or invalid
            tenantSchemaResolver.resolveSchemaName(tenantId);
            TenantContext.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or inactive tenant\"}");
        } finally {
            TenantContext.clear();
        }
    }
}

