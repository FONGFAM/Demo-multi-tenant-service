# 05 - Database Design

## 1. Kiến trúc Tổng thể (10 Bảng cốt lõi)
Hệ thống sử dụng cơ chế **Shared Database - Separate Schema**. 
Mỗi Schema của Tenant sẽ được khởi tạo với một bộ khung chuẩn mực gồm chính xác **10 Bảng** (7 bảng nghiệp vụ + 3 bảng hệ thống). Tất cả các bảng nghiệp vụ đều tuân thủ nguyên tắc "7 Cột Hệ thống + 1 Cột JSONB".

## 2. Shared Schema (`public`)
**Bảng `tenant_registry`:**
Đóng vai trò định tuyến (Routing). Map `tenant_id` từ JWT sang tên Schema vật lý (VD: `tenant_123`).

## 3. Tenant Schema (`tenant_{id}`) - 7 Bảng Nghiệp Vụ Chuẩn
Mọi bảng dưới đây đều ngầm định sở hữu **7 Trường Hệ Thống** (`id`, `owner_id`, `created_by`, `updated_by`, `created_at`, `updated_at`, `custom_properties`). Dưới đây chỉ liệt kê các trường Common mang tính nghiệp vụ (Business Common Fields).

**1. `leads` (Tiềm năng):**
- `full_name`, `email`, `phone`
- `source` (Nguồn: Web, Facebook...)
- `status` (New, Contacted, Qualified)

**2. `accounts` (Doanh nghiệp/Tổ chức):**
- `account_name`
- `website`
- `industry` (Ngành nghề)

**3. `contacts` (Cá nhân/Người liên hệ):**
- `account_id` (FK -> accounts)
- `first_name`, `last_name`, `email`, `phone`

**4. `opportunities` (Cơ hội/Giao dịch):**
- `opportunity_name`
- `account_id` (FK), `contact_id` (FK)
- `amount` (Giá trị), `stage` (Giai đoạn), `expected_close_date`

**5. `activities` (Hoạt động/Tương tác):**
- `activity_type` (CALL, MEETING, EMAIL)
- `subject`
- `target_entity` (Đa hình: leads, contacts...), `target_id`
- `due_date`, `status`

**6. `notes` (Ghi chú nội bộ):**
- `target_entity` (Đa hình), `target_id`
- `content` (TEXT)

**7. `tickets` (Hỗ trợ/Khiếu nại):**
- `subject`
- `contact_id` (FK), `account_id` (FK)
- `priority`, `status`

## 4. Tenant Schema (`tenant_{id}`) - 3 Bảng Hệ Thống (Engine)
1. **`attribute_definitions`**: Nơi khai báo Metadata để "ép kiểu" cho cột `custom_properties` của 7 bảng trên.
2. **`notification_rules`**: Định nghĩa công thức tính toán/cảnh báo (Alerts/Dashboards).
3. **`rule_execution_logs`**: Lịch sử chạy của Rule Engine.\n