# 02 - Assumptions & Open Questions

## 1. Assumptions (Giả định thiết kế)
- **A-01:** Hệ thống CRM gốc (Core CRM) đã chịu trách nhiệm cấp phát và quản lý thông tin Master của Tenant, cũng như cấp phát JWT chứa `tenant_id`.
- **A-02:** Database là PostgreSQL 15+, hỗ trợ JSONB và GIN indexing mạnh mẽ.
- **A-03:** Khối lượng dữ liệu ban đầu cho khoảng 10 tenants không vượt quá khả năng xử lý của một Connection Pool duy nhất (HikariCP).
- **A-04:** Việc chạy migration (Flyway) khi khởi tạo schema mất dưới 5 giây và không gây lock toàn cục ảnh hưởng tới các tenant khác.

## 2. Open Questions (Cần xác nhận)
| ID | Câu hỏi | Đề xuất của Architect | Trạng thái |
|---|---|---|---|
| Q-01 | Nếu một Tenant phát triển vượt bậc (Enterprise), họ có cần tách vật lý sang Database Server riêng không? | Hỗ trợ di dời Schema sang DB khác (Cấu hình Multi-Datasource trong tương lai). Hiện tại vẫn dùng chung DB. | Pending |
| Q-02 | Xử lý thế nào với dữ liệu rác (Orphaned JSONB data) khi xoá định nghĩa trong Metadata? | Chỉ Soft-Delete trong Metadata. Sử dụng background job để clean up JSONB data nếu cần. | Pending |
| Q-03 | Giới hạn số lượng Custom Fields cho mỗi Tenant là bao nhiêu? | Không giới hạn cứng về logic, nhưng khuyến cáo < 100 fields để đảm bảo hiệu năng JSONB query. | Pending |\n