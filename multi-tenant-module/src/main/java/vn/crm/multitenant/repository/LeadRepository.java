package vn.crm.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.crm.multitenant.entity.Lead;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    
    // Hỗ trợ tìm kiếm theo trường JSONB (Sử dụng cú pháp của PostgreSQL)
    // Ví dụ: Tìm các Lead có custom_properties chứa key 'nhu_cau' = 'mua_nha'
    @Query(value = "SELECT * FROM leads WHERE custom_properties @> CAST(json_build_object(:key, :value) AS jsonb)", nativeQuery = true)
    List<Lead> findByCustomProperty(@Param("key") String key, @Param("value") String value);
}
