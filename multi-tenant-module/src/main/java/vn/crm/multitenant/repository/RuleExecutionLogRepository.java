package vn.crm.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.crm.multitenant.entity.RuleExecutionLog;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleExecutionLogRepository extends JpaRepository<RuleExecutionLog, UUID> {
    
    @Query(value = "SELECT * FROM rule_execution_logs WHERE custom_properties @> CAST(json_build_object(:key, :value) AS jsonb)", nativeQuery = true)
    List<RuleExecutionLog> findByCustomProperty(@Param("key") String key, @Param("value") String value);
}
