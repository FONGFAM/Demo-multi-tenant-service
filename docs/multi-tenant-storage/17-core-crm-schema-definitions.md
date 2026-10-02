# 17 - Core CRM Schema Definitions (Zoho-Aligned)

Tài liệu này định nghĩa chính xác **các Bảng (Tables)** thuộc nhóm "Common Properties" - xương sống bắt buộc phải có của bất kỳ hệ thống CRM nào. Danh mục này được tham chiếu và chuẩn hóa theo kiến trúc nhóm phân hệ (Modules) của **Zoho CRM**. 

---

## 1. Các trường Hệ thống (System Audit Fields)
**Áp dụng cho TẤT CẢ các bảng bên dưới.** 
*   `id` (UUID) - Khóa chính.
*   `owner_id` (UUID) - ID của User chịu trách nhiệm bản ghi này.
*   `created_by`, `updated_by` (UUID).
*   `created_at`, `updated_at` (TIMESTAMPTZ).
*   `custom_properties` (JSONB) - Nơi chứa dữ liệu tùy biến theo lĩnh vực.

---

## 2. Nhóm Phân hệ SALES (Bán hàng)
*Trong Zoho CRM, nhóm Sales bao trùm toàn bộ vòng đời từ khi là người lạ đến khi chốt đơn.*

### 2.1. Bảng `leads` (Tiềm năng)
*   `full_name`, `email`, `phone` (VARCHAR)
*   `source` (VARCHAR) - Nguồn gốc.
*   `status` (VARCHAR) - Trạng thái.

### 2.2. Bảng `accounts` (Tổ chức / Doanh nghiệp)
*   `account_name` (VARCHAR) - Tên tổ chức.
*   `industry` (VARCHAR) - Lĩnh vực.

### 2.3. Bảng `contacts` (Người liên hệ / Khách hàng)
*   `account_id` (UUID FK) - Tổ chức trực thuộc.
*   `first_name`, `last_name`, `email`, `phone` (VARCHAR)

### 2.4. Bảng `opportunities` (Cơ hội kinh doanh / Deals)
*   `opportunity_name` (VARCHAR)
*   `account_id`, `contact_id` (UUID FK)
*   `amount` (DECIMAL) - Giá trị dự kiến.
*   `stage` (VARCHAR) - Giai đoạn phễu.

---

## 3. Nhóm Phân hệ ACTIVITIES (Hoạt động)
*Quản lý mọi tương tác của nhân viên.*

### 3.1. Bảng `activities` (Tasks / Calls / Events)
*   `activity_type` (VARCHAR) - CALL, MEETING, TASK.
*   `subject` (VARCHAR) - Tiêu đề.
*   `target_entity` (VARCHAR) - Trỏ tới 'leads', 'contacts'...
*   `target_id` (UUID) - ID bản ghi.
*   `status` (VARCHAR) - Open, Completed.

### 3.2. Bảng `notes` (Ghi chú)
*   `target_entity` (VARCHAR), `target_id` (UUID)
*   `content` (TEXT)

---

## 4. Nhóm Phân hệ INVENTORY (Quản lý Bán hàng / Kho)
*Quản lý sản phẩm và chứng từ tài chính.*

### 4.1. Bảng `products` (Sản phẩm / Dịch vụ)
*   `product_name`, `sku` (VARCHAR)
*   `unit_price` (DECIMAL)

### 4.2. Bảng `quotes` (Báo giá)
*   `opportunity_id` (UUID FK)
*   `total_amount` (DECIMAL)

---

## 5. Nhóm Phân hệ SUPPORT (Hỗ trợ Khách hàng)

### 5.1. Bảng `tickets` (Khiếu nại / Cases)
*   `subject` (VARCHAR)
*   `contact_id` (UUID FK)
*   `priority`, `status` (VARCHAR)

---
*Lưu ý: Bất kỳ nhóm dữ liệu nào khác ngoài các nhóm trên (Ví dụ: `hospital`, `courses`) sẽ được hệ thống linh hoạt quản lý thông qua JSONB trên các bảng lõi này, hoặc sinh ra bảng Custom mở rộng tùy ý.*
