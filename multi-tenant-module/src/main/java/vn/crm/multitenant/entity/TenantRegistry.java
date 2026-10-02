package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Bảng TenantRegistry lưu tại schema `public`.
 * Dùng để tra cứu Schema Name dựa trên Tenant ID.
 */
@Entity
@Table(name = "tenant_registry")
@Getter
@Setter
public class TenantRegistry {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(name = "tenant_id", unique = true, nullable = false)
    private String tenantId;

    @Column(name = "schema_name", nullable = false)
    private String schemaName;

    @Column(name = "is_active")
    private boolean isActive = true;
}
