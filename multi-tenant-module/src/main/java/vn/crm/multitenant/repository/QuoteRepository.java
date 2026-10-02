package vn.crm.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.crm.multitenant.entity.Quote;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    
    @Query(value = "SELECT * FROM quotes WHERE custom_properties @> CAST(json_build_object(:key, :value) AS jsonb)", nativeQuery = true)
    List<Quote> findByCustomProperty(@Param("key") String key, @Param("value") String value);
}
