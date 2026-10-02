package vn.crm.multitenant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.crm.multitenant.service.FlywayMigrationService;
import vn.crm.multitenant.validation.TenantSchemaValidator;

@Slf4j
@RestController
@RequestMapping("/internal/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final FlywayMigrationService flywayMigrationService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @PostMapping("/{tenantId}/provision")
    public ResponseEntity<String> provisionTenant(@PathVariable String tenantId) {
        try {
            TenantSchemaValidator.validateTenantId(tenantId);
        } catch (IllegalArgumentException e) {
            log.warn("Yêu cầu provision với tenantId không hợp lệ '{}': {}", tenantId, e.getMessage());
            return ResponseEntity.badRequest().body("tenantId không hợp lệ: " + e.getMessage());
        }

        // Check current status
        java.util.List<String> statuses = jdbcTemplate.query(
            "SELECT status FROM tenant_registry WHERE tenant_id = ?",
            (rs, rowNum) -> rs.getString("status"),
            tenantId
        );

        if (!statuses.isEmpty() && "ACTIVE".equals(statuses.get(0))) {
            return ResponseEntity.ok("Tenant " + tenantId + " is already active.");
        }

        String schemaName = "tenant_" + java.util.UUID.randomUUID().toString().replace("-", "");
        
        // Upsert to PROVISIONING
        jdbcTemplate.update(
            "INSERT INTO tenant_registry (id, tenant_id, schema_name, status, is_active) " +
            "VALUES (?, ?, ?, 'PROVISIONING', true) " +
            "ON CONFLICT (tenant_id) DO UPDATE SET status = 'PROVISIONING', schema_name = EXCLUDED.schema_name",
            java.util.UUID.randomUUID(), tenantId, schemaName
        );

        try {
            flywayMigrationService.initTenantSchema(schemaName);
            jdbcTemplate.update("UPDATE tenant_registry SET status = 'ACTIVE' WHERE tenant_id = ?", tenantId);
            return ResponseEntity.ok("Provisioned schema for tenant: " + tenantId);
        } catch (Exception e) {
            log.error("Lỗi khi provision tenant {}, tiến hành rollback schema", tenantId, e);
            try {
                jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            } catch (Exception dropEx) {
                log.error("Không thể rollback schema {}", schemaName, dropEx);
            }
            jdbcTemplate.update("UPDATE tenant_registry SET status = 'FAILED' WHERE tenant_id = ?", tenantId);
            return ResponseEntity.internalServerError().body("Lỗi khi provision tenant: " + e.getMessage());
        }
    }
}

