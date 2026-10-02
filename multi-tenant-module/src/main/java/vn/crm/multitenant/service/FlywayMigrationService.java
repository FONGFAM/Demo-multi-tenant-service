package vn.crm.multitenant.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Service quản lý Flyway Migration cho Public Schema và Tenant Schemas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlywayMigrationService {

    private final DataSource dataSource;

    /**
     * Chạy ngay khi Spring Boot khởi động để khởi tạo Schema Public.
     */
    @PostConstruct
    public void migratePublicSchema() {
        log.info("Bắt đầu migrate Public Schema (Bảng tenant_registry)...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Hoàn tất migrate Public Schema.");
    }

    /**
     * Gọi hàm này khi có một khách hàng (Tenant) mới đăng ký sử dụng CRM.
     * Tự động tạo Schema và đổ cấu trúc 12 Bảng vào đó.
     */
    public void initTenantSchema(String tenantSchemaName) {
        log.info("Bắt đầu khởi tạo cấu trúc cho Tenant Schema: {}", tenantSchemaName);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(tenantSchemaName) // Định tuyến Flyway chạy vào Schema mới
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Hoàn tất tạo hệ sinh thái CRM cho Tenant: {}", tenantSchemaName);
    }
}
