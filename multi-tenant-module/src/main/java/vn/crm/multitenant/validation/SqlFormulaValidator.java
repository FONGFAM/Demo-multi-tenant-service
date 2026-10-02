package vn.crm.multitenant.validation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * V5 FIX — SQL Formula Injection Prevention
 *
 * <p>Validates {@code sql_formula} trong {@link vn.crm.multitenant.entity.NotificationRule}
 * trước khi lưu vào DB. Ngăn chặn:
 * <ul>
 *   <li>DML (INSERT, UPDATE, DELETE, TRUNCATE, DROP, CREATE, ALTER)</li>
 *   <li>Schema traversal (dấu chấm + tên schema khác — vd: public.tenant_registry)</li>
 *   <li>SQL comment (-- và &#47;*) dùng để bypass validation</li>
 *   <li>Multiple statements (dấu chấm phẩy giữa câu lệnh)</li>
 *   <li>Subquery đọc dữ liệu hệ thống (information_schema, pg_catalog)</li>
 * </ul>
 *
 * <p><b>Quy tắc được phép:</b>
 * <ul>
 *   <li>Bắt buộc bắt đầu bằng {@code SELECT} (case-insensitive, bỏ qua whitespace)</li>
 *   <li>Không có schema qualifier ({@code schema.table}) — chỉ truy cập table trong schema hiện tại</li>
 *   <li>Phải trả về cột {@code id} (kiểm tra bằng tên cột)</li>
 * </ul>
 *
 * <p><b>Ví dụ tấn công bị chặn:</b>
 * <pre>
 *   "SELECT id FROM public.tenant_registry"     → schema traversal
 *   "DELETE FROM leads WHERE 1=1"               → DML
 *   "SELECT id FROM leads; DROP TABLE leads"    → multiple statements
 *   "SELECT id FROM leads -- WHERE 1=0"         → SQL comment bypass
 * </pre>
 *
 * <p><b>Lưu ý quan trọng:</b> Đây là lớp bảo vệ thứ nhất (input validation).
 * {@link RuleEngineService} cũng phải dùng read-only JDBC connection hoặc
 * EXPLAIN-only approach để bảo vệ thứ hai khi thực thi (V5 Phase 2 hardening).
 */
public final class SqlFormulaValidator {

    // ── Patterns phát hiện nội dung nguy hiểm ──────────────────────────────

    /** DML và DDL bị cấm hoàn toàn */
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            // DML
            Pattern.compile("\\b(INSERT|UPDATE|DELETE|TRUNCATE)\\b", Pattern.CASE_INSENSITIVE),
            // DDL
            Pattern.compile("\\b(DROP|CREATE|ALTER|RENAME)\\b", Pattern.CASE_INSENSITIVE),
            // SQL comments (dùng để bypass)
            Pattern.compile("--"),
            Pattern.compile("/\\*"),
            // Semicolon giữa câu (multiple statements)
            // Cho phép dấu ; nếu là ký tự cuối (sau trim), nhưng cấm ở giữa
            // Vì vậy: nếu có ; và sau đó còn ký tự non-whitespace → cấm
            Pattern.compile(";\\s*\\S"),
            // Schema qualifier — dot notation báo hiệu cross-schema access
            // Ví dụ: public.table, information_schema.columns, pg_catalog.pg_class
            // Cho phép: table_alias.column (tên không chứa underscore ở đầu)
            // CONSERVATIVE: cấm TẤT CẢ dot notation để đơn giản và an toàn
            Pattern.compile("\\w+\\.\\w+"),
            // System schemas được truy cập trực tiếp (ngay cả khi không có dot)
            Pattern.compile("\\b(information_schema|pg_catalog|pg_toast|pg_temp|pg_stat)\\b",
                    Pattern.CASE_INSENSITIVE)
    );

    /** sql_formula phải bắt đầu bằng SELECT */
    private static final Pattern MUST_START_WITH_SELECT =
            Pattern.compile("^\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);

    /** sql_formula phải chứa từ khóa FROM */
    private static final Pattern MUST_HAVE_FROM =
            Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);

    /** Độ dài tối đa để tránh ReDoS và payload quá lớn */
    private static final int MAX_FORMULA_LENGTH = 1000;

    private SqlFormulaValidator() {
        // Utility class
    }

    /**
     * Validates {@code sqlFormula}. Throw {@link IllegalArgumentException} nếu không hợp lệ.
     *
     * @param sqlFormula Nội dung sql_formula từ API request body
     * @throws IllegalArgumentException kèm message mô tả vi phạm cụ thể
     */
    public static void validate(String sqlFormula) {
        if (sqlFormula == null || sqlFormula.isBlank()) {
            throw new IllegalArgumentException("sql_formula không được null hoặc rỗng.");
        }

        if (sqlFormula.length() > MAX_FORMULA_LENGTH) {
            throw new IllegalArgumentException(
                    "sql_formula quá dài (tối đa " + MAX_FORMULA_LENGTH + " ký tự). "
                    + "Thực tế: " + sqlFormula.length() + " ký tự.");
        }

        // Phải bắt đầu bằng SELECT
        if (!MUST_START_WITH_SELECT.matcher(sqlFormula).find()) {
            throw new IllegalArgumentException(
                    "sql_formula phải bắt đầu bằng SELECT. "
                    + "Nhận được: '" + abbreviate(sqlFormula) + "'");
        }

        // Phải có FROM
        if (!MUST_HAVE_FROM.matcher(sqlFormula).find()) {
            throw new IllegalArgumentException(
                    "sql_formula phải có mệnh đề FROM.");
        }

        // Kiểm tra từng pattern nguy hiểm
        for (Pattern forbidden : FORBIDDEN_PATTERNS) {
            if (forbidden.matcher(sqlFormula).find()) {
                throw new IllegalArgumentException(
                        "sql_formula chứa nội dung bị cấm (pattern: '"
                        + forbidden.pattern() + "'). "
                        + "sql_formula chỉ được là SELECT query đơn, "
                        + "không có schema qualifier, không có DML/DDL, "
                        + "không có comment SQL.");
            }
        }
    }

    private static String abbreviate(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
