package vn.crm.multitenant.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import vn.crm.multitenant.validation.TenantSchemaValidator;

/**
 * Bắt lấy Connection từ Pool và SET SCHEMA cho Postgresql dựa trên Tenant ID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource dataSource;

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
        // V4 FIX: validate schema name trước khi dùng trong SQL động
        // Đây là last line of defense tại tầng Hibernate — ngăn SQL injection qua TenantContext
        try {
            TenantSchemaValidator.validate(tenantIdentifier);
        } catch (IllegalArgumentException e) {
            log.error("Schema name không hợp lệ, từ chối kết nối: '{}' — {}", tenantIdentifier, e.getMessage());
            throw new SQLException("Invalid tenant schema: " + e.getMessage(), e);
        }

        final Connection connection = getAnyConnection();
        // Cốt lõi của kiến trúc Shared DB - Separate Schema
        // Đổi schema về đúng tenant_id (VD: tenant_123)
        try {
            connection.createStatement().execute("SET SCHEMA '" + tenantIdentifier + "'");
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
