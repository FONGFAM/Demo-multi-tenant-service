# 09 - API Design

## 1. Nguyên tắc chung
- Tất cả API đều yêu cầu Header `Authorization: Bearer <JWT>`. JWT chứa `tenant_id`.
- Base URL: `/api/v1/tenant-data`

## 2. Các API cốt lõi

### 2.1. Metadata API (Attribute Definition)
`GET /metadata/attributes`
- **Mục đích:** Lấy danh sách định nghĩa trường dữ liệu.
- **Response:**
  ```json
  [
    {"id": "uuid", "field_key": "nhom_mau", "type": "STRING", "required": true}
  ]
  ```

`POST /metadata/attributes`
- **Mục đích:** Thêm/Sửa một định nghĩa trường.
- **Request:**
  ```json
  {"field_key": "bien_so", "type": "STRING", "required": false}
  ```

### 2.2. Data Records API
`POST /{target_table}`
- **Mục đích:** Tạo bản ghi mới. Dữ liệu sẽ đi qua Validation Engine.
- **Request:**
  ```json
  {
    "name": "Nguyen Van A", 
    "email": "a@gmail.com",
    "custom_properties": {
      "nhom_mau": "O",
      "bien_so": "29A-12345"
    }
  }
  ```

`GET /{target_table}`
- **Mục đích:** Lấy danh sách bản ghi, hỗ trợ filter trên custom fields.
- **Query Params:** `?page=0&size=20&filter.custom_properties.nhom_mau=O`\n