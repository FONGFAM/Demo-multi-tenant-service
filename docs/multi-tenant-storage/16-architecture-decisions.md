# 16 - Architecture Decisions Records (ADR)

## ADR-01: Lựa chọn Mô hình Multi-Tenant
- **Quyết định:** Sử dụng `Shared Database - Separate Schema`.
- **Lý do:** Cách ly vật lý tốt, backup dễ dàng.

## ADR-02: Lựa chọn Lưu trữ Dữ liệu Tuỳ biến
- **Quyết định:** Triển khai **Bộ bảng CRM Chuẩn** (customers, leads, opportunities...), mỗi bảng có sẵn cột `custom_properties` JSONB.
- **Lý do:** Đảm bảo hệ thống vẫn giữ được xương sống của một CRM (với các trường Common chuẩn và Khóa ngoại chặt chẽ), nhưng lại có thể "biến hình" để phục vụ bất kỳ lĩnh vực nào (Y tế, Giáo dục) bằng cách định nghĩa thêm thuộc tính vào cột JSONB tương ứng.

## ADR-03: Loại bỏ Bảng Dữ liệu Dẫn xuất vật lý
- **Quyết định:** Dùng `Dynamic Rule Engine`. Lưu câu lệnh SQL vào `notification_rules`, chạy on-the-fly kết hợp In-memory Cache. Không tạo các bảng `derived_metrics` vật lý.
- **Lý do:** Tối giản Database, chống lỗi lệch dữ liệu (Stale Data), dễ thay đổi công thức.\n