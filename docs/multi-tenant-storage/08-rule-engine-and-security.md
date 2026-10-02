# 08 - Rule Engine & Security

## 1. Dynamic Alerting Rule Engine
Bảng `notification_rules`:
- `rule_code`: 'BIRTHDAY_ALERT'
- `condition_sql`: `EXTRACT(MONTH FROM CAST(custom_properties->>'dob' AS DATE)) = EXTRACT(MONTH FROM CURRENT_DATE)`
- `message_template`: 'Chúc mừng sinh nhật {name}'

## 2. Security & SQL Injection Prevention
Lưu mệnh đề SQL trong cơ sở dữ liệu có rủi ro Injection. Phương án phòng ngừa:
1. **Quyền quản trị:** Bảng `notification_rules` chỉ do System Admin hoặc Developer cập nhật (Trusted Metadata). UI không phơi bày tính năng cho phép End-user nhập SQL thô.
2. **Read-Only Execution:** User Database mà Spring Boot sử dụng để chạy Cron Job chỉ được phân quyền `SELECT` trên các bảng dữ liệu, bị tước toàn bộ quyền `DROP, DELETE, UPDATE`.
3. **Cross-Tenant Leakage:** Mệnh đề SQL không chứa `tenant_id`. Việc cách ly do Hibernate `SET SCHEMA` đảm nhiệm ở tầng connection, đảm bảo Job chỉ chạy giới hạn trong schema hiện tại.\n