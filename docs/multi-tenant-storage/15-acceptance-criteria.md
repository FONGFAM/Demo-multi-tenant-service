# 15 - Acceptance Criteria

## 1. Tiêu chí chức năng
- [ ] Khi gọi API CRUD với JWT của Tenant A, dữ liệu chỉ lưu vào Schema A.
- [ ] Gửi JSON payload chứa field không có trong `attribute_definition` -> API trả về 400 Bad Request.
- [ ] Gửi JSON payload sai kiểu dữ liệu (VD: String thay vì Number) -> API trả về 400 Bad Request.
- [ ] Cron Job chạy đúng giờ, query đúng kết quả từ bảng Rules.

## 2. Tiêu chí phi chức năng
- [ ] Query tìm kiếm trên trường custom (JSONB) với bảng có 100k dòng mất dưới 200ms (Đã dùng GIN Index).
- [ ] Hệ thống chịu được 100 concurrent requests gọi API biểu đồ nhờ Cache.
- [ ] Log hệ thống in ra rõ ràng ID của Tenant thực hiện request.\n