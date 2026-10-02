package vn.crm.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.crm.multitenant.entity.AttributeDefinition;

import java.util.List;
import java.util.UUID;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {
    List<AttributeDefinition> findByEntityType(String entityType);
}
