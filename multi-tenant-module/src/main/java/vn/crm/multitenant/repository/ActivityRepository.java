package vn.crm.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.crm.multitenant.entity.Activity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    
    @Query(value = "SELECT * FROM activities WHERE custom_properties @> CAST(json_build_object(:key, :value) AS jsonb)", nativeQuery = true)
    List<Activity> findByCustomProperty(@Param("key") String key, @Param("value") String value);
}
