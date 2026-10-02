package vn.crm.multitenant.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class HibernateConfig implements HibernatePropertiesCustomizer {

    private final CurrentTenantIdentifierResolver tenantIdentifierResolver;
    private final MultiTenantConnectionProvider tenantConnectionProvider;

    public HibernateConfig(CurrentTenantIdentifierResolver tenantIdentifierResolver,
                           MultiTenantConnectionProvider tenantConnectionProvider) {
        this.tenantIdentifierResolver = tenantIdentifierResolver;
        this.tenantConnectionProvider = tenantConnectionProvider;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, tenantConnectionProvider);
    }
}
