package vn.crm.multitenant;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Multi-Tenant Isolation Test Suite")
class MultiTenantIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("crm_db_test")
            .withUsername("crm_user")
            .withPassword("crm_pass");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Force small connection pool for TC-ISO-13
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private vn.crm.multitenant.repository.LeadRepository leadRepository;
    @Autowired
    private vn.crm.multitenant.service.RuleEngineService ruleEngineService;

    @Autowired
    private vn.crm.multitenant.context.TenantSchemaResolver tenantSchemaResolver;

    @LocalServerPort
    private int port;

    private String tokenAlpha;
    private String tokenBeta;

    @BeforeAll
    void provisionTenantsAndLogin() {
        ResponseEntity<String> provA = rest.postForEntity(
                "/api/tenants/alpha/provision", null, String.class);
        assertThat(provA.getStatusCode()).isEqualTo(OK);

        ResponseEntity<String> provB = rest.postForEntity(
                "/api/tenants/beta/provision", null, String.class);
        assertThat(provB.getStatusCode()).isEqualTo(OK);

        // TODO: thay bằng provisioning thật ở Phase 1b
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(
            new org.springframework.jdbc.datasource.SingleConnectionDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), true)
        );
        jdbc.update("INSERT INTO public.tenant_registry (id, tenant_id, schema_name, is_active) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING", java.util.UUID.randomUUID(), "tenant_alpha", "tenant_alpha", true);
        jdbc.update("INSERT INTO public.tenant_registry (id, tenant_id, schema_name, is_active) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING", java.util.UUID.randomUUID(), "tenant_beta", "tenant_beta", true);

        tokenAlpha = "tenant_alpha";
        tokenBeta  = "tenant_beta";
    }


    private HttpHeaders withTenantHeader(String tenantId) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (tenantId != null && !tenantId.isEmpty()) {
            h.set("X-Tenant-ID", tenantId);
        }
        return h;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<Map> createLead(String tenantId, String fullName) {
        String body = String.format("{\"fullName\":\"%s\",\"status\":\"NEW\"}", fullName.replace("\"", "\\\""));
        return rest.exchange("/api/v1/leads", POST, new HttpEntity<>(body, withTenantHeader(tenantId)), Map.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<List> getLeads(String tenantId) {
        return rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader(tenantId)), List.class);
    }

    @Test
    @DisplayName("TC-ISO-01 [ENABLED] Alpha data must NOT be visible to Beta (core schema isolation)")
    void tc_iso_01_alpha_data_invisible_to_beta() {
        String uniqueName = "ISOLATION-LEAD-" + UUID.randomUUID();
        createLead(tokenAlpha, uniqueName);
        ResponseEntity<List> betaLeads = getLeads(tokenBeta);
        boolean found = Optional.ofNullable(betaLeads.getBody()).orElse(Collections.emptyList()).stream()
                .anyMatch(item -> uniqueName.equals(((Map<?, ?>) item).get("fullName")));
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("TC-ISO-02 [ENABLED] Alpha can read its own data within its schema")
    void tc_iso_02_alpha_can_read_own_data() {
        String uniqueName = "ALPHA-OWN-" + UUID.randomUUID();
        createLead(tokenAlpha, uniqueName);
        ResponseEntity<List> alphaLeads = getLeads(tokenAlpha);
        boolean found = Optional.ofNullable(alphaLeads.getBody()).orElse(Collections.emptyList()).stream()
                .anyMatch(item -> uniqueName.equals(((Map<?, ?>) item).get("fullName")));
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("TC-ISO-12 [ENABLED] Alpha attribute definitions do NOT affect Beta validation")
    void tc_iso_12_attribute_definitions_scoped_per_tenant() {
        String attrBody = "{\"entityType\":\"LEAD\",\"attributeKey\":\"alpha_only_required_field\",\"dataType\":\"STRING\",\"isRequired\":true}";
        rest.exchange("/api/v1/attributes", POST, new HttpEntity<>(attrBody, withTenantHeader(tokenAlpha)), Map.class);
        ResponseEntity<Map> betaLead = createLead(tokenBeta, "Beta Lead Without Alpha Field");
        assertThat(betaLead.getStatusCode()).isEqualTo(OK);
    }

    @Test
    @DisplayName("TC-ISO-10 [V3] Provisioning requires authentication")
    @Disabled("Phase 1b - sẽ viết lại cho /internal + service token")
    void tc_iso_10_provisioning_requires_authentication() {
        ResponseEntity<String> resp = rest.postForEntity("/api/tenants/unauthenticated_attempt/provision", null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @DisplayName("TC-ISO-11 [NEW] Missing or empty X-Tenant-ID header returns 400")
    void tc_iso_11_missing_tenant_header_returns_400() {
        ResponseEntity<String> resp = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader(null)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
        
        ResponseEntity<String> resp2 = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader("")), String.class);
        assertThat(resp2.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-ISO-03 [NEW] Non-existent tenant returns 403")
    void tc_iso_03_nonexistent_tenant_returns_403() {
        ResponseEntity<String> resp = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader("not_a_tenant")), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("TC-ISO-04 [NEW] System schema names (public, pg_catalog) return 403")
    void tc_iso_04_system_tenant_returns_403() {
        ResponseEntity<String> resp = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader("public")), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(FORBIDDEN);
        
        ResponseEntity<String> resp2 = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader("pg_catalog")), String.class);
        assertThat(resp2.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("TC-ISO-05 [ENABLED] [V4] SQL injection in schema name is rejected by validator")
    void tc_iso_05_schema_injection_is_rejected() {
        String injectionPayload = "alpha\" DROP SCHEMA tenant_alpha --";
        ResponseEntity<String> resp = rest.postForEntity("/api/tenants/{tid}/provision", null, String.class, injectionPayload);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(resp.getBody()).contains("Schema name không hợp lệ");
        
        // Unit test direct Validator
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> vn.crm.multitenant.validation.TenantSchemaValidator.validate("alpha; DROP DATABASE"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> vn.crm.multitenant.validation.TenantSchemaValidator.validate("public"));
    }

    @Test
    @DisplayName("TC-ISO-06 [ENABLED] [V5] Cross-schema sql_formula rejected on save")
    // NOTE: Sẽ xóa ở Phase 2 (Rule Engine cleanup)
    void tc_iso_06_cross_schema_sql_formula_is_rejected() {
        String maliciousRule = "{\"targetTable\":\"leads\",\"ruleType\":\"ALERT\",\"sqlFormula\":\"SELECT id FROM public.tenant_registry\"}";
        ResponseEntity<String> resp = rest.exchange("/api/v1/notification_rules", POST, new HttpEntity<>(maliciousRule, withTenantHeader(tokenAlpha)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-ISO-07 [ENABLED] [V5] DML sql_formula rejected on save")
    // NOTE: Sẽ xóa ở Phase 2 (Rule Engine cleanup)
    void tc_iso_07_dml_sql_formula_is_rejected() {
        String dmlRule = "{\"targetTable\":\"leads\",\"ruleType\":\"ALERT\",\"sqlFormula\":\"DELETE FROM leads WHERE 1=1\"}";
        ResponseEntity<String> resp = rest.exchange("/api/v1/notification_rules", POST, new HttpEntity<>(dmlRule, withTenantHeader(tokenAlpha)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-ISO-14 [NEW] Deactivated tenant rejected (403) and cache eviction test")
    void tc_iso_14_deactivated_tenant_is_fully_blocked() {
        rest.postForEntity("/api/tenants/gamma/provision", null, String.class);
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(
            new org.springframework.jdbc.datasource.SingleConnectionDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), true)
        );
        // Initially active
        jdbc.update("INSERT INTO public.tenant_registry (id, tenant_id, schema_name, is_active) VALUES (?, ?, ?, ?) ON CONFLICT (tenant_id) DO UPDATE SET is_active = EXCLUDED.is_active", java.util.UUID.randomUUID(), "gamma", "tenant_gamma", true);
        
        // 1. Should be OK initially
        ResponseEntity<String> resp1 = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader("gamma")), String.class);
        assertThat(resp1.getStatusCode()).isEqualTo(OK);

        // 2. Deactivate in DB
        jdbc.update("UPDATE public.tenant_registry SET is_active = false WHERE tenant_id = 'gamma'");
        
        // 3. Since cached, it still works for a while, but if we evict it:
        tenantSchemaResolver.evict("gamma");
        
        // 4. Now it should be blocked (403)
        ResponseEntity<String> resp2 = rest.exchange("/api/v1/leads", GET, new HttpEntity<>(withTenantHeader("gamma")), String.class);
        assertThat(resp2.getStatusCode()).isEqualTo(FORBIDDEN);
    }

    @Test
    @DisplayName("TC-ISO-13 [ENABLED] Concurrent requests do not cross-contaminate schemas")
    void tc_iso_13_concurrent_no_schema_leak() throws Exception {
        int threads = 20;
        int loops = 5;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        java.util.concurrent.atomic.AtomicBoolean hasLeak = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        createLead("tenant_alpha", "MARKER_ALPHA");
        createLead("tenant_beta", "MARKER_BETA");

        for (int i = 0; i < threads; i++) {
            boolean isAlpha = (i % 2 == 0);
            String tenantId = isAlpha ? tokenAlpha : tokenBeta;
            String wrongMarker = isAlpha ? "MARKER_BETA" : "MARKER_ALPHA";

            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < loops; j++) {
                        ResponseEntity<List> leads = getLeads(tenantId);
                        if (leads.getStatusCode() != OK) {
                            hasLeak.set(true);
                        } else {
                            boolean leak = Optional.ofNullable(leads.getBody()).orElse(Collections.emptyList()).stream()
                                .anyMatch(item -> wrongMarker.equals(((Map<?, ?>) item).get("fullName")));
                            if (leak) {
                                hasLeak.set(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    hasLeak.set(true);
                }
            }, executor));
        }

        latch.countDown();
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        executor.shutdown();

        assertThat(hasLeak.get()).as("Concurrency caused schema leak!").isFalse();
    }

    @AfterEach
    void tearDown() {
        vn.crm.multitenant.context.TenantContext.clear();
    }

    @Test
    @DisplayName("TC-ISO-16 [ENABLED] Cross-schema IDOR via Repository is prevented")
    void tc_iso_16_idor_via_repository() {
        vn.crm.multitenant.context.TenantContext.setTenantId("tenant_alpha");
        vn.crm.multitenant.entity.Lead alphaLead = new vn.crm.multitenant.entity.Lead();
        alphaLead.setFullName("Alpha Secret Lead");
        alphaLead = leadRepository.save(alphaLead);
        UUID alphaLeadId = alphaLead.getId();

        // Positive check: Alpha can find it
        Optional<vn.crm.multitenant.entity.Lead> alphaReadsAlpha = leadRepository.findById(alphaLeadId);
        assertThat(alphaReadsAlpha).isPresent();

        vn.crm.multitenant.context.TenantContext.setTenantId("tenant_beta");
        Optional<vn.crm.multitenant.entity.Lead> betaReadsAlpha = leadRepository.findById(alphaLeadId);
        
        assertThat(betaReadsAlpha).isEmpty();
    }

    @Test
    @DisplayName("TC-ISO-17 [ENABLED] TenantContext fail-closed when empty")
    void tc_iso_17_tenant_context_fail_closed() {
        vn.crm.multitenant.context.TenantContext.clear();
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class, 
            vn.crm.multitenant.context.TenantContext::getTenantId, 
            "Phải throw exception thay vì trả về 'public'"
        );
    }
    
    @Test
    @DisplayName("TC-ISO-18 [ENABLED] TenantContextFilter clears context under all scenarios")
    void tc_iso_18_filter_clears_context() throws Exception {
        vn.crm.multitenant.security.TenantContextFilter filter = new vn.crm.multitenant.security.TenantContextFilter(tenantSchemaResolver);
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        
        // Scenario 1: missing header
        boolean[] chainCalled1 = {false};
        filter.doFilter(request, response, (req, res) -> { chainCalled1[0] = true; });
        assertThat(chainCalled1[0]).isFalse();
        assertThat(vn.crm.multitenant.context.TenantContext.isPresent()).isFalse();

        // Scenario 2: invalid tenant
        request.addHeader("X-Tenant-ID", "invalid");
        boolean[] chainCalled2 = {false};
        filter.doFilter(request, response, (req, res) -> { chainCalled2[0] = true; });
        assertThat(chainCalled2[0]).isFalse();
        assertThat(vn.crm.multitenant.context.TenantContext.isPresent()).isFalse();

        // Scenario 3: chain success
        request.removeHeader("X-Tenant-ID");
        request.addHeader("X-Tenant-ID", "tenant_alpha");
        boolean[] chainCalled3 = {false};
        filter.doFilter(request, response, (req, res) -> {
            chainCalled3[0] = true;
            assertThat(vn.crm.multitenant.context.TenantContext.getTenantId()).isEqualTo("tenant_alpha");
        });
        assertThat(chainCalled3[0]).isTrue();
        assertThat(vn.crm.multitenant.context.TenantContext.isPresent()).isFalse();
        
        // Scenario 4: chain throws
        request.removeHeader("X-Tenant-ID");
        request.addHeader("X-Tenant-ID", "tenant_alpha");
        boolean[] chainCalled4 = {false};
        try {
            filter.doFilter(request, response, (req, res) -> {
                chainCalled4[0] = true;
                assertThat(vn.crm.multitenant.context.TenantContext.getTenantId()).isEqualTo("tenant_alpha");
                throw new RuntimeException("Simulated exception in chain");
            });
        } catch (RuntimeException e) {}
        assertThat(chainCalled4[0]).isTrue();
        assertThat(vn.crm.multitenant.context.TenantContext.isPresent()).isFalse();
    }

    @Test
    @DisplayName("TC-ISO-19 [NEW] Repository call without context throws explicit exception")
    void tc_iso_19_repository_call_without_context_throws_exception() {
        vn.crm.multitenant.context.TenantContext.clear();
        
        Exception ex = assertThrows(Exception.class, () -> {
            leadRepository.findAll();
        });
        
        boolean found = false;
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("No tenant context available to acquire connection")) {
                found = true;
                break;
            }
            current = current.getCause();
        }
        assertThat(found).isTrue();
    }
}

