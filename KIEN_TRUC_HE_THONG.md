# BẢN THIẾT KẾ KIẾN TRÚC HỆ THỐNG MULTI-TENANT

**Tài liệu Mật độ cao (Siêu chi tiết) dùng cho Báo cáo / Bảo vệ Đồ án**

---

## 1. MỤC TIÊU VÀ ĐỊNH VỊ HỆ THỐNG CỐT LÕI

* **Bối cảnh:** Phát triển ứng dụng SaaS (Software as a Service) hướng tới tập doanh nghiệp nhỏ, quy mô khoảng 10 khách hàng (Tenants). Đòi hỏi tối giản hóa chi phí hạ tầng (Sử dụng Oracle Cloud Free Tier) và giảm thiểu tối đa khối lượng công việc bảo trì cho đội ngũ vận hành.
* **Định vị Module:** Hệ thống này được thiết kế theo tư duy **Module Lõi (Flexible Data Engine Module)**. Nó hoàn toàn có thể chạy độc lập hoặc "cắm" (plug-and-play) vào một hệ sinh thái Microservices CRM có sẵn. Do hệ thống CRM gốc đã đảm nhiệm việc quản lý danh sách khách hàng (`tenants`), Module này sẽ **KHÔNG** lưu trữ trùng lặp dữ liệu khách hàng.
* **Triết lý kiến trúc:** Tối giản (Minimalism) - Tái sử dụng tối đa (Reuse) - Kiến trúc hướng Siêu dữ liệu (Metadata-Driven).

---

## 2. CHIẾN LƯỢC LƯU TRỮ VÀ CÔ LẬP DỮ LIỆU (DATA ISOLATION)

Hệ thống sử dụng hệ quản trị **PostgreSQL 15+** với kiến trúc **Schema-per-Tenant**. Đây là điểm cân bằng hoàn hảo giữa hiệu năng và chi phí.

* **Tính riêng tư tuyệt đối:** Dữ liệu của mỗi khách hàng (Tenant) được mã nguồn tạo ra một Schema vật lý hoàn toàn cách ly (VD: `schema_acme`, `schema_globex`). Các truy vấn của khách hàng A sẽ không bao giờ có thể "vô tình" đọc được dữ liệu của khách hàng B, triệt tiêu rủi ro rò rỉ dữ liệu B2B chéo.
* **Cấp phát tài nguyên tự động (Dynamic Provisioning):** Hệ thống không yêu cầu Database Admin (DBA) phải vào tạo Schema bằng tay. Khi API Gateway gửi tín hiệu có Tenant mới, mã nguồn Java sẽ kích hoạt thư viện **Programmatic Flyway** để tự động chạy các tệp Migration SQL (`V1__init_tenant_schema.sql`), khởi tạo toàn bộ không gian làm việc cho khách hàng mới ngay trong lúc Runtime (Lúc ứng dụng đang chạy).

---

## 3. CƠ CHẾ CHUYỂN MẠCH TỰ ĐỘNG (AUTOMATIC SCHEMA ROUTING)

Để hệ thống Java nhận biết được Request thuộc về ai và tự động trỏ vào đúng Schema, chúng tôi áp dụng chuỗi công nghệ 3 lớp:

1. **Lớp Xác thực (Authentication Filter):**
   * Mọi HTTP Request gửi từ Frontend đều phải đi kèm JWT Token.
   * Spring Security Filter (sử dụng thư viện `jjwt`) bóc tách Token để trích xuất Claim `tenant_id`.
   * Gắn `tenant_id` này vào biến cục bộ **Java ThreadLocal**. Điều này đảm bảo an toàn luồng (Thread-safe) trong môi trường xử lý đa luồng của Spring Boot.
2. **Lớp Đánh chặn (Hibernate Interceptor):**
   * Ghi đè Interface `CurrentTenantIdentifierResolver` để báo cho Hibernate biết luồng hiện tại đang phục vụ cho `tenant_id` nào.
3. **Lớp Chuyển vùng (Connection Provider):**
   * Ghi đè Interface `MultiTenantConnectionProvider`. Trước khi Hibernate thực thi bất kỳ câu lệnh SQL (SELECT, UPDATE, INSERT) nào, nó sẽ thò tay vào HikariCP (Connection Pool) lấy ra một Connection.
   * Ngay tại khoảnh khắc đó, Hibernate tự động bơm câu lệnh `SET SCHEMA 'tenant_id'` vào Connection. Database lập tức chuyển ngữ cảnh làm việc sang Schema của khách hàng đó.

---

## 4. KIẾN TRÚC DỮ LIỆU ĐỘNG (METADATA-DRIVEN CUSTOM FIELDS)

Bài toán: *Mỗi khách hàng muốn quản lý thông tin nhân viên theo một kiểu khác nhau. Khách A cần "Nhóm máu", khách B cần "Biển số xe".*
Thay vì dùng mô hình EAV (Entity-Attribute-Value) cũ kỹ gây chậm Database, hay DDL (`ALTER TABLE`) gây nguy hiểm sập hệ thống, kiến trúc mới sử dụng:

* **Cột lưu trữ vạn năng (JSONB):** Mọi trường tùy biến được gom chung vào duy nhất 1 cột vật lý tên là `custom_properties` có định dạng **JSONB**. Khác với chuỗi JSON thường (String), JSONB là định dạng nhị phân, được PostgreSQL hỗ trợ native, cho tốc độ đọc ghi cực nhanh.
* **Siêu dữ liệu định hướng (Metadata Data Contract):** Cấu trúc của cục JSON không bị thả nổi, mà bị kiểm soát chặt chẽ bởi bảng `custom_field_definitions` (Định nghĩa siêu dữ liệu).
  ```sql
  CREATE TABLE custom_field_definitions (
      id UUID,
      field_key VARCHAR,    -- VD: 'nhom_mau'
      field_type VARCHAR,   -- VD: 'STRING', 'NUMBER', 'DATE'
      is_required BOOLEAN
  );
  ```
* **Động cơ kiểm duyệt (Validation Engine):** Khi JSON Payload bay từ UI xuống, Java không dùng câu lệnh `IF/ELSE` cứng. Nó bốc siêu dữ liệu từ **Caffeine Cache** (Thời gian phản hồi 0.001ms), sau đó đối chiếu linh hoạt (Dynamic Validation) với cấu trúc của JSON Payload. Nếu đúng luật mới cho phép lưu.
* **Chỉ mục tìm kiếm (GIN Index):** Việc truy vấn vào trong JSONB (VD: Tìm người có `nhom_mau = 'O'`) được tối ưu hóa bằng **GIN Index** (Generalized Inverted Index) và extension **pg_trgm**, giúp chuyển bài toán duyệt mảng (O(N)) thành duyệt cây nhị phân (O(log N)).

---

## 5. XỬ LÝ DỮ LIỆU DẪN XUẤT VÀ SỰ KIỆN (DYNAMIC RULE ENGINE)

Đây là tư duy kiến trúc ưu việt nhất của hệ thống. Nhằm loại bỏ hoàn toàn tình trạng phình to Database do tạo bảng ảo (Virtual View), bảng tạm (Materialized View) hay bảng dữ liệu dẫn xuất (`derived_metrics`), hệ thống quy về **MỘT CƠ CHẾ DUY NHẤT**: Lấy mã code SQL làm Dữ liệu.

Hệ thống thiết lập một Bảng điều khiển trung tâm:

```sql
CREATE TABLE business_rules (
    id UUID PRIMARY KEY,
    rule_type VARCHAR,        -- Phân loại: 'DASHBOARD', 'REPORT', 'ALERT'
    rule_name VARCHAR,        -- Tên: 'Tổng doanh thu', 'Chúc mừng sinh nhật'
    sql_query TEXT            -- Lõi thực thi: Chứa câu lệnh SQL hoàn chỉnh
);
```

Cơ chế này giải quyết mượt mà 3 bài toán lớn:

1. **Vẽ Biểu đồ Real-time (rule_type = 'DASHBOARD'):**
   * API Backend kéo `sql_query` ra và chọc thẳng xuống Database (On-the-fly). Tốc độ được duy trì nhờ **Covering Index** (Chỉ mục bao phủ có `INCLUDE`).
   * Bảo vệ Database bằng **Micro-Caching (Caffeine)** với TTL 15 giây. Trong 15 giây đó, 10.000 người vào xem biểu đồ thì hệ thống chỉ tốn đúng 1 Query duy nhất xuống DB, chống hoàn toàn hiệu ứng Bão truy cập (Cache Stampede).
2. **Xuất báo cáo cuối năm (rule_type = 'REPORT'):**
   * Logic tương tự Dashboard, nhưng không dùng Cache ngắn hạn. Người dùng bấm xuất, hệ thống trích xuất `sql_query` thực thi và trả File Excel. Tuyệt đối không lưu dữ liệu dẫn xuất tính toán ra Bảng vật lý.
3. **Hệ thống cảnh báo thời gian (rule_type = 'ALERT'):**
   * Để xử lý các sự kiện biến thiên theo thời gian (VD: Sáng nay tới sinh nhật nhân viên A, hợp đồng của B hết hạn), hệ thống cài đặt một **Spring @Scheduled (Cron Job)** thức dậy vào 2h00 sáng mỗi ngày.
   * Job sẽ quét toàn bộ các luật thuộc nhóm 'ALERT', lôi `sql_query` ra chạy. Hễ ai thỏa mãn điều kiện, Java sẽ kích hoạt **Spring ApplicationEvent** (Event-Driven) để gọi hàm gửi Email/Push Notification dưới Background, không làm tắc nghẽn luồng chính.

---

## 6. TỔNG KẾT STACK CÔNG NGHỆ CHỐT HẠ

* **Lõi hệ thống:** Java 17+, Spring Boot 3.x
* **Cơ sở dữ liệu:** PostgreSQL 15+
* **ORM Framework:** Hibernate 6, Spring Data JPA
* **Xử lý JSONB (Bắt buộc):** Thư viện `hypersistence-utils` của Vlad Mihalcea
* **Quản lý phiên bản DB:** `flyway-core`
* **Quản lý bộ nhớ đệm (In-memory Cache):** `Caffeine Cache`
* **Bảo mật:** `jjwt-api`, `jjwt-impl` (Xử lý chuỗi Token)

Kiến trúc này đảm bảo 3 yếu tố khắt khe nhất của đồ án: **Không tốn chi phí hạ tầng vật lý thừa thãi - Hiệu năng xử lý siêu việt nhờ Index và Cache - Khả năng tùy biến vô hạn (Extensibility) cho khách hàng mà không cần sửa code.**
