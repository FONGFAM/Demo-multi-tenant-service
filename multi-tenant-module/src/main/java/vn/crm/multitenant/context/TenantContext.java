package vn.crm.multitenant.context;

import lombok.extern.slf4j.Slf4j;

/**
 * Lưu trữ Tenant ID cho Thread hiện tại (thường là request hiện tại).
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    public static final String DEFAULT_TENANT_ID = "public";

    public static void setTenantId(String tenantId) {
        log.debug("Setting tenantId to {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext is empty - no tenant bound to current thread");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean isPresent() {
        return CURRENT_TENANT.get() != null;
    }
}
