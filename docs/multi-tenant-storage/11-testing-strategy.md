# 11 - Testing Strategy

## 1. Unit Testing
- **Validation Engine:** Mock Metadata Cache. Test các case gửi JSON sai kiểu, thiếu required field, JSON rỗng. Phải ném `ConstraintViolationException`.
- **SQL Generator:** Test hàm ráp nối chuỗi SQL từ bảng Rule xem có đúng chuẩn không, có bị SQL Injection không (VD: nhập ký tự lạ vào rule).

## 2. Integration Testing (TestContainers)
- **Tenant Isolation Test:** Sử dụng PostgreSQL TestContainer. Tạo 2 tenant. Insert dữ liệu vào Tenant A. Query ở context Tenant B. Xác nhận kết quả trả về rỗng (Đảm bảo Schema Isolation hoạt động).
- **JSONB Search Test:** Insert 10.000 records với JSONB. Chạy query tìm kiếm xem GIN Index có hit không (phân tích EXPLAIN ANALYZE).

## 3. Load Testing
- Kịch bản bão truy cập (Cache Stampede) vào API biểu đồ. Đảm bảo Caffeine Cache giúp Database chỉ chịu tải 1 query/giây.\n