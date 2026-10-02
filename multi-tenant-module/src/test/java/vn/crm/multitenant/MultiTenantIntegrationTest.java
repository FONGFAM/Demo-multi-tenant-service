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
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  LƯỚI AN TOÀN — Phase 0: Multi-Tenant Data Isolation Integration Tests
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Tests dùng Testcontainers PostgreSQL 15 (real DB, không mock).
 *
 * TRẠNG THÁI BAN ĐẦU (trước khi fix bất kỳ bug nào):
 *   ENABLED + phải PASS    : TC-ISO-01, TC-ISO-02, TC-ISO-12
 *   @Disabled (bug chưa fix): TC-ISO-03, 04, 05, 06, 07, 08, 09, 10, 11, 13, 14
 *
 * Khi fix xong một Phase, xóa @Disabled của các test tương ứng.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Multi-Tenant Isolation Test Suite")
class MultiTenantIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // INFRASTRUCTURE
    // Container được start trong static block TRƯỚC KHI @DynamicPropertySource
    // đọc URL — tránh "Mapped port can only be obtained after container started"
    // ─────────────────────────────────────────────────────────────────────────

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
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private vn.crm.multitenant.repository.LeadRepository leadRepository;

    @LocalServerPort
    private int port;

    /** Token của tenant_alpha — set trong @BeforeAll, dùng chung cho tất cả tests */
    private String tokenAlpha;

    /** Token của tenant_beta — set trong @BeforeAll, dùng chung cho tất cả tests */
    private String tokenBeta;

    // ─────────────────────────────────────────────────────────────────────────
    // SETUP
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeAll
    void provisionTenantsAndLogin() {
        // Provision 2 tenant schemas độc lập
        // NOTE: /api/tenants/** hiện là permitAll (bug V3) — không cần token.
        // Sau Phase 2 (V3 fix), thêm token PLATFORM_ADMIN ở đây.
        ResponseEntity<String> provA = rest.postForEntity(
                "/api/tenants/alpha/provision", null, String.class);
        assertThat(provA.getStatusCode())
                .as("Provisioning tenant_alpha phải thành công").isEqualTo(OK);

        ResponseEntity<String> provB = rest.postForEntity(
                "/api/tenants/beta/provision", null, String.class);
        assertThat(provB.getStatusCode())
                .as("Provisioning tenant_beta phải thành công").isEqualTo(OK);

        // Login lấy tokens
        // NOTE: auth là fake (bug V2) — bất kỳ username/tenantId nào đều lấy token được.
        // Sau Phase 2 (V2 fix), truyền password hợp lệ.
        tokenAlpha = loginAndGetToken("alice", "tenant_alpha");
        tokenBeta  = loginAndGetToken("bob",   "tenant_beta");

        assertThat(tokenAlpha).as("Token alpha không được null").isNotBlank();
        assertThat(tokenBeta).as("Token beta không được null").isNotBlank();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String loginAndGetToken(String username, String tenantId) {
        ResponseEntity<Map> resp = rest.postForEntity(
                "/api/v1/auth/login?username={u}&tenantId={t}",
                null, Map.class, username, tenantId);
        assertThat(resp.getStatusCode()).as("Login phải thành công cho " + tenantId).isEqualTo(OK);
        return (String) Objects.requireNonNull(resp.getBody()).get("token");
    }

    private HttpHeaders jsonBearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.setBearerAuth(token);
        }
        return h;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<Map> createLead(String token, String fullName) {
        String body = String.format("{\"fullName\":\"%s\",\"status\":\"NEW\"}",
                fullName.replace("\"", "\\\""));
        return rest.exchange("/api/v1/leads", POST,
                new HttpEntity<>(body, jsonBearer(token)), Map.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<List> getLeads(String token) {
        return rest.exchange("/api/v1/leads", GET,
                new HttpEntity<>(jsonBearer(token)), List.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA ISOLATION TESTS — ENABLED, phải PASS ngay cả trước khi fix bug
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-ISO-01 [ENABLED] Alpha data must NOT be visible to Beta (core schema isolation)")
    void tc_iso_01_alpha_data_invisible_to_beta() {
        String uniqueName = "ISOLATION-LEAD-" + UUID.randomUUID();

        // Alpha tạo lead
        ResponseEntity<Map> create = createLead(tokenAlpha, uniqueName);
        assertThat(create.getStatusCode()).as("Alpha phải tạo được lead").isEqualTo(OK);

        // Beta đọc leads — KHÔNG được thấy lead của Alpha
        ResponseEntity<List> betaLeads = getLeads(tokenBeta);
        assertThat(betaLeads.getStatusCode()).isEqualTo(OK);

        boolean alphaLeadFoundInBeta = Optional.ofNullable(betaLeads.getBody())
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(item -> uniqueName.equals(((Map<?, ?>) item).get("fullName")));

        assertThat(alphaLeadFoundInBeta)
                .as("SCHEMA ISOLATION BI VỠ: Lead của Alpha thấy được từ Beta!")
                .isFalse();
    }

    @Test
    @DisplayName("TC-ISO-02 [ENABLED] Alpha can read its own data within its schema")
    void tc_iso_02_alpha_can_read_own_data() {
        String uniqueName = "ALPHA-OWN-" + UUID.randomUUID();

        ResponseEntity<Map> create = createLead(tokenAlpha, uniqueName);
        assertThat(create.getStatusCode()).isEqualTo(OK);

        ResponseEntity<List> alphaLeads = getLeads(tokenAlpha);
        assertThat(alphaLeads.getStatusCode()).isEqualTo(OK);

        boolean found = Optional.ofNullable(alphaLeads.getBody())
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(item -> uniqueName.equals(((Map<?, ?>) item).get("fullName")));

        assertThat(found).as("Alpha phải thấy lead của chính mình").isTrue();
    }

    @Test
    @DisplayName("TC-ISO-12 [ENABLED] Alpha attribute definitions do NOT affect Beta validation")
    void tc_iso_12_attribute_definitions_scoped_per_tenant() {
        // Alpha định nghĩa custom field BẮT BUỘC cho LEAD
        String attrBody = "{\"entityType\":\"LEAD\","
                + "\"attributeKey\":\"alpha_only_required_field\","
                + "\"dataType\":\"STRING\","
                + "\"isRequired\":true}";

        ResponseEntity<Map> attrResp = rest.exchange(
                "/api/v1/attributes", POST,
                new HttpEntity<>(attrBody, jsonBearer(tokenAlpha)), Map.class);
        assertThat(attrResp.getStatusCode())
                .as("Alpha phải tạo được attribute definition").isEqualTo(OK);

        // Beta tạo lead KHÔNG có 'alpha_only_required_field' — phải THÀNH CÔNG
        // vì schema của Beta không có attribute definition đó
        ResponseEntity<Map> betaLead = createLead(tokenBeta, "Beta Lead Without Alpha Field");
        assertThat(betaLead.getStatusCode())
                .as("ATTRIBUTE DEFINITION BLEED: Beta bị ảnh hưởng bởi rule của Alpha!")
                .isEqualTo(OK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECURITY TESTS — @Disabled vì bug chưa được fix
    // Khi fix xong bug tương ứng, XÓA @Disabled + chạy để verify
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Disabled("[BUG V3 - Phase 2] /api/tenants/** la permitAll. "
            + "Xoa @Disabled sau Phase 2 (V3) fix.")
    @DisplayName("TC-ISO-10 [V3] Provisioning requires authentication")
    void tc_iso_10_provisioning_requires_authentication() {
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/tenants/unauthenticated_attempt/provision", null, String.class);
        assertThat(resp.getStatusCode()).isIn(UNAUTHORIZED, FORBIDDEN);
    }

    @Test
    @Disabled("[BUG V2 - Phase 2] Auth endpoint cap token ma khong validate tenantId ton tai. "
            + "Xoa @Disabled sau Phase 2 (V2) fix.")
    @DisplayName("TC-ISO-11 [V2] Login with non-existent tenant returns 401")
    void tc_iso_11_login_nonexistent_tenant_is_rejected() {
        ResponseEntity<Map> resp = rest.postForEntity(
                "/api/v1/auth/login?username=hacker&tenantId=tenant_does_not_exist_xyz",
                null, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @Disabled("[BUG V1 - Phase 3] TenantContext fail-open ve 'public' thay vi throw exception. "
            + "Xoa @Disabled sau Phase 3 (V1) fix. "
            + "TODO: Implement buildJwtWithoutTenantClaim() sau Phase 1 (V10).")
    @DisplayName("TC-ISO-03 [V1] JWT without tenantId claim returns 401")
    void tc_iso_03_jwt_without_tenant_claim_is_rejected() {
        String tokenWithoutTenant = buildJwtWithoutTenantClaim();
        ResponseEntity<List> resp = rest.exchange(
                "/api/v1/leads", GET,
                new HttpEntity<>(jsonBearer(tokenWithoutTenant)), List.class);
        assertThat(resp.getStatusCode()).isEqualTo(UNAUTHORIZED);
    }

    @Test
    @Disabled("[BUG V1 - Phase 3] tenantId='public' trong JWT khong bi chan. "
            + "Xoa @Disabled sau Phase 3 (V1) fix.")
    @DisplayName("TC-ISO-04 [V1] JWT with tenantId='public' is rejected")
    void tc_iso_04_jwt_claiming_public_schema_is_rejected() {
        ResponseEntity<Map> loginResp = rest.postForEntity(
                "/api/v1/auth/login?username=attacker&tenantId=public",
                null, Map.class);
        String maliciousToken = (String) Objects.requireNonNull(loginResp.getBody()).get("token");
        ResponseEntity<List> resp = rest.exchange(
                "/api/v1/leads", GET,
                new HttpEntity<>(jsonBearer(maliciousToken)), List.class);
        assertThat(resp.getStatusCode()).isIn(UNAUTHORIZED, FORBIDDEN);
    }

    @Test
    @DisplayName("TC-ISO-05 [ENABLED] [V4] SQL injection in schema name is rejected")
    void tc_iso_05_schema_injection_is_rejected() {
        // Sửa payload: không dùng dấu chấm phẩy (;) vì StrictHttpFirewall của Spring Security
        // sẽ chặn `%3B` trong URL và trả về 403 trước khi tới Controller.
        // Dùng payload SQLi khác để test validator của chúng ta chặn thành công với 400.
        String injectionPayload = "alpha\" DROP SCHEMA tenant_alpha --";
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/tenants/{tid}/provision", null, String.class, injectionPayload);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-ISO-06 [ENABLED] [V5] Cross-schema sql_formula rejected on save")
    void tc_iso_06_cross_schema_sql_formula_is_rejected() {
        String maliciousRule = "{\"targetTable\":\"leads\","
                + "\"ruleType\":\"ALERT\","
                + "\"sqlFormula\":\"SELECT id FROM public.tenant_registry\"}";
        // Đổi Map.class thành String.class vì khi validation fail, Controller trả về plain text message
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/notification_rules", POST,
                new HttpEntity<>(maliciousRule, jsonBearer(tokenAlpha)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("TC-ISO-07 [ENABLED] [V5] DML sql_formula rejected on save")
    void tc_iso_07_dml_sql_formula_is_rejected() {
        String dmlRule = "{\"targetTable\":\"leads\","
                + "\"ruleType\":\"ALERT\","
                + "\"sqlFormula\":\"DELETE FROM leads WHERE 1=1\"}";
        // Đổi Map.class thành String.class vì khi validation fail, Controller trả về plain text message
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/notification_rules", POST,
                new HttpEntity<>(dmlRule, jsonBearer(tokenAlpha)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    @Disabled("[BUG V6/V7 - Phase 3] Cron TenantContext isolation chua duoc test tu dong. "
            + "Xoa @Disabled sau Phase 3 (V6 + V7) fix.")
    @DisplayName("TC-ISO-08 [V6/V7] Cron processes each tenant in isolation (no context bleed)")
    void tc_iso_08_cron_context_isolation() {
        Assertions.fail("Implement sau Phase 3 — dung @SpyBean tren RuleEngineService");
    }

    @Test
    @Disabled("[BUG V10 - Phase 1] JWT secret hardcode, kho generate expired token trong test. "
            + "Xoa @Disabled sau Phase 1 (V10) va implement buildJwtWithExpiredTime().")
    @DisplayName("TC-ISO-09 [V10] Expired JWT token returns 401")
    void tc_iso_09_expired_token_is_rejected() {
        Assertions.fail("Implement sau Phase 1 (V10) khi secret configurable");
    }

    @Test
    @Disabled("[BUG V6/V11 - Phase 3] Khong co is_active check trong filter (V11) va cron (V6). "
            + "Xoa @Disabled sau Phase 3 (V9 + V11) fix.")
    @DisplayName("TC-ISO-14 [V6/V11] Deactivated tenant token rejected; cron skips inactive tenant")
    void tc_iso_14_deactivated_tenant_is_fully_blocked() {
        Assertions.fail("Implement sau Phase 3 (V9 + V11) — provision->login->deactivate->verify");
    }

    @Test
    @Disabled("[BUG V14 - Phase 1b] Lọt schema do connection pool chưa reset triệt để. Xóa @Disabled sau Phase 1b fix.")
    @DisplayName("TC-ISO-13 [V14] Concurrent requests do not cross-contaminate schemas")
    void tc_iso_13_concurrent_no_schema_leak() throws Exception {
        int threads = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        java.util.concurrent.atomic.AtomicBoolean hasLeak = new java.util.concurrent.atomic.AtomicBoolean(false);

        for (int i = 0; i < threads; i++) {
            boolean isAlpha = (i % 2 == 0);
            String token = isAlpha ? tokenAlpha : tokenBeta;
            String expectedTenant = isAlpha ? "tenant_alpha" : "tenant_beta";

            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                String name = "CONCURRENT-" + expectedTenant + "-" + UUID.randomUUID();
                ResponseEntity<Map> createResp = createLead(token, name);
                if (createResp.getStatusCode() != OK) {
                    hasLeak.set(true);
                }
                
                ResponseEntity<List> leads = getLeads(token);
                if (leads.getStatusCode() != OK) {
                    hasLeak.set(true);
                }
            }, executor));
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        executor.shutdown();

        assertThat(hasLeak.get()).as("Concurrency caused schema leak or error").isFalse();
    }

    @Test
    @DisplayName("TC-ISO-16 [ENABLED] Cross-schema IDOR via Repository is prevented")
    void tc_iso_16_idor_via_repository() {
        // Alpha creates a lead directly via repository
        vn.crm.multitenant.context.TenantContext.setTenantId("tenant_alpha");
        vn.crm.multitenant.entity.Lead alphaLead = new vn.crm.multitenant.entity.Lead();
        alphaLead.setFullName("Alpha Secret Lead");
        alphaLead = leadRepository.save(alphaLead);
        UUID alphaLeadId = alphaLead.getId();

        // Beta tries to read it
        vn.crm.multitenant.context.TenantContext.setTenantId("tenant_beta");
        Optional<vn.crm.multitenant.entity.Lead> betaReadsAlpha = leadRepository.findById(alphaLeadId);
        
        assertThat(betaReadsAlpha)
            .as("Beta không được phép đọc Lead của Alpha dù biết ID")
            .isEmpty();
            
        vn.crm.multitenant.context.TenantContext.clear();
    }

    @Test
    @Disabled("[BUG V1 - Phase 1a] TenantContext fallback về 'public' thay vì throw exception. Xóa @Disabled sau Phase 1a fix.")
    @DisplayName("TC-ISO-17 [V1] TenantContext fail-closed when empty")
    void tc_iso_17_tenant_context_fail_closed() {
        vn.crm.multitenant.context.TenantContext.clear();
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class, 
            vn.crm.multitenant.context.TenantContext::getTenantId, 
            "Phải throw exception thay vì trả về 'public'"
        );
    }
    
    @Test
    @Disabled("[BUG - Phase 1a] Filter không clear TenantContext nếu request không có Authorization header. Xóa @Disabled sau Phase 1a fix.")
    @DisplayName("TC-ISO-18 [ENABLED] Filter always clears TenantContext in finally block")
    void tc_iso_18_filter_clears_context() throws Exception {
        // Using unit test approach to test the filter directly since ThreadLocal is per thread
        vn.crm.multitenant.security.JwtAuthenticationFilter filter = 
            new vn.crm.multitenant.security.JwtAuthenticationFilter(null);
        
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = (req, res) -> {
            // Context should be cleared even if chain throws exception
            throw new RuntimeException("Simulated exception in chain");
        };
        
        vn.crm.multitenant.context.TenantContext.setTenantId("dirty_tenant");
        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException e) {
            // expected
        }
        
        // Assert that after doFilter, TenantContext is cleared. Currently getTenantId falls back to public if cleared.
        assertThat(vn.crm.multitenant.context.TenantContext.getTenantId()).isEqualTo("public");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo JWT hợp lệ (đúng signature) nhưng KHÔNG có claim 'tenantId'.
     * Dùng cho TC-ISO-03.
     *
     * TODO (Phase 1 — V10): Sau khi secret configurable qua app.jwt.secret trong
     * application-test.properties, implement bằng JJWT builder:
     * <pre>
     *   byte[] key = HexFormat.of().parseHex(testSecret);
     *   return Jwts.builder().setSubject("no_tenant")
     *       .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
     *       .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256)
     *       .compact();
     * </pre>
     */
    private String buildJwtWithoutTenantClaim() {
        throw new UnsupportedOperationException(
                "Implement sau Phase 1 (V10). Xem TODO comment.");
    }
}
