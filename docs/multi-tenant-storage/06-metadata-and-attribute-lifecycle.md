# 06 - Metadata & Attribute Lifecycle

## 1. Trách nhiệm của Metadata
Bảng `attribute_definitions` định nghĩa "Hợp đồng dữ liệu" (Data Contract) cho cột JSONB `custom_properties` của các bảng CRM chuẩn (VD: `customers`, `leads`).

## 2. Quản lý Phiên bản (Versioning) & Lịch sử
Mỗi khi có sự thay đổi logic rủi ro cao (Đổi is_required, Đổi Type), hệ thống tăng `version` của field.
- **Bảng `attribute_history_logs`:**
  - `id` (UUID PK)
  - `attribute_id` (UUID FK)
  - `changed_by` (UUID)
  - `changed_at` (TIMESTAMPTZ)
  - `old_state` (JSONB)
  - `new_state` (JSONB)

## 3. Vòng đời thuộc tính (States)
1. **DRAFT:** Vừa tạo, cho phép cấu hình tự do. Validation Engine bỏ qua.
2. **ACTIVE:** Cấm đổi `field_type` nếu đã có dữ liệu. Validation Engine áp dụng luật.
3. **DEPRECATED:** Đánh dấu sắp bỏ. UI hiển thị warning. API vẫn nhận dữ liệu nhưng không bắt buộc.
4. **RETIRED:** Validation Engine bỏ qua. API trả về lỗi nếu cố ghi đè.

## 4. Cô lập Tenant (Tenant Isolation)
Mỗi tenant tự định nghĩa siêu dữ liệu cho riêng mình. Cùng là thực thể 'ticket', Tenant A có thể có trường 'priority', Tenant B có trường 'sla_deadline'. Việc schema tách biệt loại bỏ rủi ro xung đột `field_key` giữa các tenant.\n