# 13 - Implementation Roadmap

## Phase 1: Nền tảng Tenant & Database (Tuần 1)
- Setup Spring Boot, HikariCP, PostgreSQL.
- Viết Filter lấy JWT, ThreadLocal.
- Cấu hình Hibernate Interceptor (`SET SCHEMA`).
- Tích hợp Flyway Programmatic để sinh schema tự động.

## Phase 2: Metadata & JSONB Core (Tuần 2)
- Tạo bảng `attribute_definition` và Entity với JSONB.
- Viết API quản lý Metadata.
- Xây dựng Validation Engine tích hợp Caffeine Cache.
- Viết CRUD API cho Data Records.

## Phase 3: Rule Engine & Tối ưu hoá (Tuần 3)
- Tạo bảng `notification_rules`.
- Viết `@Scheduled` job và SQL Generator.
- Tích hợp Spring Events bắn notification.
- Tối ưu GIN Index, Covering Index.
- Hoàn thiện Test và Tài liệu.\n