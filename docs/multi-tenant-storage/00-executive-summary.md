# 00 - Executive Summary

## 1. Mục tiêu
Hệ thống CRM hiện hữu cần một giải pháp lưu trữ linh hoạt cho phép đa dạng khách hàng (tenants) từ các lĩnh vực khác nhau (y tế, giáo dục...) lưu trữ dữ liệu tuỳ chỉnh mà không làm ảnh hưởng đến cấu trúc lõi. Module này được xây dựng độc lập để đáp ứng yêu cầu trên.

## 2. Giải pháp cốt lõi
- **Kiến trúc dữ liệu:** `Shared Database - Separate Schema` trên PostgreSQL. Mỗi tenant sở hữu một schema riêng biệt.
- **Dữ liệu linh hoạt:** Dùng `JSONB` kết hợp `GIN Index` lưu trữ custom properties, giải quyết bài toán không giới hạn trường thông tin mà vẫn đảm bảo tốc độ tìm kiếm.
- **Kiểm soát tính toàn vẹn:** `Metadata-Driven Architecture`. Việc xác thực dữ liệu tuỳ biến được cấu hình thông qua bảng `attribute_definition`, không hard-code trong ứng dụng.
- **Dữ liệu dẫn xuất & Sự kiện:** Áp dụng `Metadata-driven rule engine` và `Scheduled polling` (Cron jobs) để tính toán on-the-fly và kích hoạt cảnh báo, loại bỏ hoàn toàn các bảng vật lý lưu trữ dữ liệu dẫn xuất dễ biến động.

## 3. Lợi ích
- **Cô lập bảo mật:** Dữ liệu tenant tách biệt vật lý ở tầng schema.
- **Chi phí tối ưu:** Sử dụng chung một Database Instance và Connection Pool, tối đa hoá tài nguyên.
- **Khả năng mở rộng:** Tính năng Rule Engine cho phép admin định nghĩa vô hạn các cảnh báo và báo cáo mà không cần deploy lại mã nguồn.\n