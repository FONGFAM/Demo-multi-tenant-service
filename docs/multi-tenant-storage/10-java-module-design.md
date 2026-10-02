# 10 - Java Module Design

## 1. Cấu trúc Package (Spring Boot)
```text
vn.crm.multitenant.data
├── config/           # Hibernate, Security, Cache, Flyway config
├── context/          # ThreadLocalTenantContext, Interceptors
├── controller/       # REST APIs
├── entity/           # JPA Entities (Common Fields + JSONB)
├── metadata/         # Metadata Service & Validation Engine
├── repository/       # Spring Data JPA Repositories
├── ruleengine/       # Scheduled Jobs, SQL Generator
└── service/          # Business logic
```

## 2. Trách nhiệm các thành phần
- **`TenantContextFilter`:** Filter ở tần HTTP, trích xuất `tenant_id` từ Token và set vào `ThreadLocal`.
- **`TenantConnectionProvider`:** Cấp phát connection từ HikariCP và chạy `SET SCHEMA '...'`.
- **`MetadataValidationService`:** Chạy trước khi gọi Repository. Lưu trữ Metadata trong `CaffeineCacheManager`.
- **`NotificationJob`:** Khai báo `@Scheduled`, query bảng `notification_rules` và publish `Spring ApplicationEvent`.\n