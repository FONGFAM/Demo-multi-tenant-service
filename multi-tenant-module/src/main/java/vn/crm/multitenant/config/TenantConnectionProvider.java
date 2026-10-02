package vn.crm.multitenant.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import vn.crm.multitenant.validation.TenantSchemaValidator;

import vn.crm.multitenant.context.TenantSchemaResolver;

/**
 * Bắt lấy Connection từ Pool và SET SCHEMA cho Postgresql dựa trên Tenant ID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource dataSource;
    private final TenantSchemaResolver tenantSchemaResolver;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        if ("UNKNOWN_TENANT_SENTINEL".equals(tenantIdentifier)) {
            throw new org.hibernate.HibernateException("No tenant context available to acquire connection");
        }

        // V4 FIX: validate schema name trước khi dùng trong SQL động
        // Thay vì dùng tenantId trực tiếp, dùng Schema Name từ Registry
        String schemaName;
        try {
            schemaName = tenantSchemaResolver.resolveSchemaName(tenantIdentifier);
            TenantSchemaValidator.validate(schemaName);
        } catch (IllegalArgumentException e) {
            log.error("Lỗi xác định schema cho tenant: '{}' — {}", tenantIdentifier, e.getMessage());
            throw new SQLException("Invalid tenant schema: " + e.getMessage(), e);
        }


        final Connection connection = getAnyConnection();
        // Cốt lõi của kiến trúc Shared DB - Separate Schema
        // Đổi schema về đúng schemaName (VD: tenant_123_schema)
        try {
            connection.createStatement().execute("SET SCHEMA '" + schemaName + "'");
        } catch (SQLException e) {
            log.error("Lỗi khi set schema cho tenant {}: {}", tenantIdentifier, e.getMessage());
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reset schema về public trước khi trả Connection về Pool
            connection.createStatement().execute("SET SCHEMA 'public'");
        } catch (SQLException e) {
            log.error("Lỗi khi reset schema về public", e);
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
