package vn.crm.multitenant.validation;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * V4 FIX — Schema Name Injection Prevention
 *
 * <p>Validates tenant schema names trước khi dùng trong câu lệnh SQL động
 * (SET SCHEMA '...' và Flyway migration). Ngăn chặn SQL injection qua path variable.
 *
 * <p><b>Ví dụ tấn công bị chặn:</b>
 * <pre>
 *   POST /api/tenants/alpha'; DROP SCHEMA tenant_beta; --/provision
 * </pre>
 *
 * <p><b>Quy tắc:</b>
 * <ul>
 *   <li>Chỉ cho phép chữ thường, số, và dấu gạch dưới</li>
 *   <li>Độ dài 1–63 ký tự (giới hạn identifier của PostgreSQL)</li>
 *   <li>Blacklist: public, information_schema, pg_catalog, pg_toast, pg_temp</li>
 * </ul>
 */
public final class TenantSchemaValidator {

    /** Pattern chỉ cho phép lowercase letters, digits, underscores — an toàn cho PostgreSQL identifier */
    private static final Pattern SAFE_SCHEMA_PATTERN = Pattern.compile("^[a-z0-9_]{1,63}$");

    /** Tên schema hệ thống bị cấm — tenant KHÔNG được đặt tên trùng */
    private static final Set<String> SYSTEM_SCHEMA_BLACKLIST = Set.of(
            "public",
            "information_schema",
            "pg_catalog",
            "pg_toast",
            "pg_temp"
    );

    private TenantSchemaValidator() {
        // Utility class — không khởi tạo
    }

    /**
     * Kiểm tra xem {@code schemaName} có hợp lệ để dùng trong SQL động không.
     *
     * @param schemaName Tên schema cần kiểm tra (đã bao gồm prefix "tenant_" nếu có)
     * @throws IllegalArgumentException nếu tên không hợp lệ, kèm message mô tả lý do
     */
    public static void validate(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Schema name không được null hoặc rỗng.");
        }

        if (!SAFE_SCHEMA_PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException(
                    "Schema name không hợp lệ: '" + schemaName + "'. "
                    + "Chỉ cho phép chữ thường (a-z), số (0-9), và dấu gạch dưới (_). "
                    + "Độ dài tối đa 63 ký tự.");
        }

        if (SYSTEM_SCHEMA_BLACKLIST.contains(schemaName.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Schema name '" + schemaName + "' là tên schema hệ thống bị cấm.");
        }
    }

    /**
     * Kiểm tra xem tenantId (chưa có prefix) có hợp lệ không.
     * Gọi method này trước khi ghép thành "tenant_" + tenantId.
     *
     * @param tenantId Giá trị tenantId từ path variable hoặc JWT claim
     * @throws IllegalArgumentException nếu tenantId không hợp lệ
     */
    public static void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId không được null hoặc rỗng.");
        }
        // Validate luôn full schema name (tenant_ + tenantId)
        validate("tenant_" + tenantId);
    }
}
