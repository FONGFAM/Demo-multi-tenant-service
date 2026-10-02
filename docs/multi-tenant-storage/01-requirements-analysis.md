# 01 - Requirements Analysis

## 1. Tổng quan
Module Multi-Tenant Storage đóng vai trò là "Data Engine" cho hệ thống CRM hiện tại, cho phép mở rộng schema dữ liệu động theo từng khách hàng.

## 2. Functional Requirements (FR)
| ID | Yêu cầu | Mô tả chi tiết |
|---|---|---|
| FR-01 | Quản lý Tenant Schema | Hệ thống tự động provision schema mới khi có tenant đăng ký. |
| FR-02 | Lưu trữ Common Data | Hỗ trợ CRUD các trường chuẩn (ID, Name, Email, TaxCode, Address). |
| FR-03 | Lưu trữ Custom Data | Hỗ trợ CRUD các trường tuỳ biến không giới hạn số lượng và kiểu. |
| FR-04 | Metadata Management | Cung cấp CRUD cho `attribute_definition` để định nghĩa custom fields (Type, Required, Default). |
| FR-05 | Data Validation | Tự động validate custom data dựa trên metadata trước khi lưu. |
| FR-06 | Advanced Search | Cho phép query/filter kết hợp giữa Common và Custom properties. |
| FR-07 | Derived Data Rules | Hỗ trợ cấu hình các rule tính toán/báo cáo từ dữ liệu gốc. |
| FR-08 | Notification Engine | Hỗ trợ quét dữ liệu định kỳ (Scheduled polling) dựa trên Rule Engine để phát cảnh báo. |

## 3. Non-Functional Requirements (NFR)
| ID | Yêu cầu | Ngưỡng / Tiêu chí |
|---|---|---|
| NFR-01 | Performance | API Read/Write < 200ms (P95). Query JSONB search < 500ms. |
| NFR-02 | Isolation | Dữ liệu tenant A tuyệt đối không lọt sang tenant B (Schema isolation). |
| NFR-03 | Scalability | Xử lý được dữ liệu cho 10+ tenants trên cùng 1 DB instance mà không nghẽn. |
| NFR-04 | Extensibility | Có thể thêm rule mới (Alerts/Reports) mà không cần downtime. |

## 4. Phạm vi (In/Out of Scope)
- **In-Scope:** Lưu trữ, truy xuất dữ liệu động, Metadata validation, Cảnh báo Scheduled polling, API REST.
- **Out-of-Scope:** Quản lý User/Role của CRM, Thanh toán, UI/UX Frontend, Quản trị Tenant gốc (Module chỉ nhận `tenant_id`).\n