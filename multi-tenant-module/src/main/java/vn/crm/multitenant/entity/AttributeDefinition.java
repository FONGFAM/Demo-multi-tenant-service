package vn.crm.multitenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attribute_definitions")
@Getter
@Setter
public class AttributeDefinition {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "target_table", nullable = false)
    private String entityType; // e.g., 'CUSTOMER'

    @Column(name = "field_key", nullable = false)
    private String attributeKey; // e.g., 'zalo_id', 'membership_tier'

    @Column(name = "field_type", nullable = false)
    private String dataType; // e.g., 'STRING', 'NUMBER', 'BOOLEAN'

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
