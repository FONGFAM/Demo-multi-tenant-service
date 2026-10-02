# 07 - Derived Data Engine

## 1. Phân loại và Định nghĩa Dữ liệu dẫn xuất
Dữ liệu dẫn xuất là thông tin không được sinh ra từ User Input, mà tính toán từ Dữ liệu gốc (Common + Custom Properties).
- **Loại 1: Metric / Aggregation (Số liệu tức thời):** VD: Tổng giá trị hợp đồng trong tháng.
- **Loại 2: Alerts / Events (Sự kiện):** VD: Ngày mai tới hạn chót thanh toán.

## 2. Kiến trúc không lưu vật lý (On-the-fly & Metadata Driven)
Hệ thống **không sử dụng** các bảng `derived_metrics` lặp lại cho từng thực thể để tránh Write Amplification (Dữ liệu gốc đổi -> Cập nhật 100 bảng dẫn xuất liên quan -> Nghẽn Database).

Mô hình Dữ liệu:
### 2.1. Bảng `notification_rules`
| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| id | UUID PK | |
| target_table | VARCHAR | Thực thể chịu tác động |
| rule_type | VARCHAR | 'AGGREGATION' hoặc 'ALERT' |
| rule_name | VARCHAR | Tên luật |
| sql_formula| TEXT | Công thức SQL động |
| status | VARCHAR | Trạng thái (ACTIVE, PAUSED) |
| trigger_cron| VARCHAR | Định kỳ chạy (Dành cho ALERT) |

### 2.2. Bảng `rule_execution_logs` (Lịch sử tính toán)
Thay vì lưu kết quả vật lý, hệ thống lưu vết các lần chạy Rule để Audit và Observability.
| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| id | UUID PK | |
| rule_id | UUID FK | Rule nào được chạy |
| executed_at | TIMESTAMPTZ | Thời điểm chạy |
| execution_ms| INT | Thời gian chạy (giám sát timeout) |
| affected_rows| INT | Số lượng bản ghi thỏa mãn/thay đổi |
| status | VARCHAR | 'SUCCESS', 'FAILED' |
| error_msg | TEXT | Chi tiết lỗi nếu có |

## 3. Cơ chế phụ thuộc dữ liệu (Data Dependencies)
Công thức `sql_formula` sẽ tham chiếu trực tiếp đến `custom_properties` của các bảng CRM (VD: `customers`, `deals`). 
- Nếu đổi công thức -> Chạy lại lấy kết quả mới ngay lập tức (Do không lưu vật lý).
- Kết quả được đệm trên **Caffeine Cache** (In-memory) trong 30s-1 phút đối với các Query AGGREGATION để giảm tải truy xuất lặp lại.\n