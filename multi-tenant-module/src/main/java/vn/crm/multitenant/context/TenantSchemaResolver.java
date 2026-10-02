package vn.crm.multitenant.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TenantSchemaResolver {

    private final JdbcTemplate jdbcTemplate;
    private final Cache<String, String> cache;

    public TenantSchemaResolver(JdbcTemplate jdbcTemplate, 
                                @Value("${tenant.cache.ttl:30}") long ttlSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    public String resolveSchemaName(String tenantId) {
        if ("public".equalsIgnoreCase(tenantId) || "pg_catalog".equalsIgnoreCase(tenantId)) {
            throw new IllegalArgumentException("System schema names are not allowed as tenant");
        }
        if ("UNKNOWN_TENANT_SENTINEL".equals(tenantId)) {
            return "public"; // Sentinel uses public for startup
        }
        return cache.get(tenantId, this::fetchFromDb);
    }

    private String fetchFromDb(String tenantId) {
        List<String> results = jdbcTemplate.query(
            "SELECT schema_name FROM public.tenant_registry WHERE tenant_id = ? AND status = 'ACTIVE' AND is_active = true",
            (rs, rowNum) -> rs.getString("schema_name"),
            tenantId
        );
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found or inactive: " + tenantId);
        }
        return results.get(0);
    }

    public void evict(String tenantId) {
        cache.invalidate(tenantId);
    }
}
