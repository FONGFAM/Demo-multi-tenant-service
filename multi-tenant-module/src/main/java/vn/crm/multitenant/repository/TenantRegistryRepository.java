package vn.crm.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.crm.multitenant.entity.TenantRegistry;

import java.util.UUID;

@Repository
public interface TenantRegistryRepository extends JpaRepository<TenantRegistry, UUID> {
}
