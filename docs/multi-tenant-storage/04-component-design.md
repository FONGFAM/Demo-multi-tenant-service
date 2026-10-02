# 04 - Component Design

## 1. Các thành phần chính (Core Components)

### A. Tenant Context Resolution
- Trích xuất `tenant_id` từ JWT.
- Quản lý `ThreadLocalTenantContext`.

### B. Schema Provisioning Engine
- Khi có sự kiện `TenantCreatedEvent` (từ Core CRM), gọi Programmatic Flyway để chạy script `V1__init_tenant.sql`.

### C. Metadata Management Service
- Cung cấp CRUD cho bảng `attribute_definition`.
- Tích hợp `Caffeine Cache` để lưu trữ cấu hình Metadata trên RAM (TTL 10 phút), tránh việc query bảng metadata liên tục.

### D. Dynamic Validation Engine
- Interceptor tại tầng Service. Trộn JSON Payload với Metadata Cache để bắt lỗi (Data Types, Required, Enums) trước khi đẩy xuống Repository.

### E. Scheduled Rule Engine (Alerts & Derived Data)
- Spring `@Scheduled` Job chạy định kỳ.
- Đọc bảng `notification_rules`. Trích xuất mệnh đề `condition_sql`.
- Gắn mệnh đề vào câu SQL động, query Database, kích hoạt `ApplicationEvent` nếu thoả mãn.\n