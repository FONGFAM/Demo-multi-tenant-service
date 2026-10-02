package vn.crm.multitenant.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import vn.crm.multitenant.context.TenantContext;

import java.util.Map;

/**
 * Giúp Hibernate biết được câu lệnh SQL hiện tại đang dành cho Tenant nào.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getTenantId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
