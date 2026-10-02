# 03 - Architecture Overview

## 1. System Context
Module hoạt động như một REST API Service độc lập. 
- **Upstream:** Frontend SPA, API Gateway, Core CRM Services.
- **Downstream:** PostgreSQL Database.

## 2. Architectural Patterns
1. **Schema-per-Tenant:** Cách ly vật lý luồng dữ liệu ở tầng Database Schema.
2. **Metadata-Driven Architecture:** Giao diện và cấu trúc dữ liệu bị chi phối bởi siêu dữ liệu cấu hình tại bảng `attribute_definition`.
3. **Dynamic Rule Engine:** Xử lý sự kiện (cảnh báo/báo cáo) bằng cách lưu trữ mệnh đề SQL trong DB và thực thi thông qua Scheduled Cron Jobs.

## 3. Data Flow
1. API Gateway xác thực Request, nhúng `tenant_id` vào Header (JWT).
2. Module nhận Request, Spring Security Filter nạp `tenant_id` vào `ThreadLocal`.
3. Hibernate chặn (Intercept) Connection, gọi lệnh `SET SCHEMA 'tenant_id'`.
4. Logic thao tác trên các bảng trong Schema hiện tại. Dữ liệu Custom được chuyển đổi qua lại giữa Java Map và JSONB nhờ thư viện `hypersistence-utils`.\n