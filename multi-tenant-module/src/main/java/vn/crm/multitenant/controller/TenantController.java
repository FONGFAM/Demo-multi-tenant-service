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
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final FlywayMigrationService flywayMigrationService;

    @PostMapping("/{tenantId}/provision")
    public ResponseEntity<String> provisionTenant(@PathVariable String tenantId) {
        // V4 FIX: validate tenantId trước khi dùng trong SQL động (SET SCHEMA)
        try {
            TenantSchemaValidator.validateTenantId(tenantId);
        } catch (IllegalArgumentException e) {
            log.warn("Yêu cầu provision với tenantId không hợp lệ '{}': {}", tenantId, e.getMessage());
            return ResponseEntity.badRequest().body("tenantId không hợp lệ: " + e.getMessage());
        }

        flywayMigrationService.initTenantSchema("tenant_" + tenantId);
        return ResponseEntity.ok("Provisioned schema for tenant: " + tenantId);
    }
}

