# 14 - Task Breakdown

| Task ID | Component | Tên Task | Phụ thuộc | DoD |
|---|---|---|---|---|
| TSK-01 | Config | Setup Hibernate Multi-tenant | None | Chạy được query tự động switch schema. |
| TSK-02 | Config | Tích hợp Flyway tạo schema on-the-fly | TSK-01 | Gọi API `/provision` sinh ra schema mới dưới DB. |
| TSK-03 | Metadata | CRUD API cho `attribute_definition` | TSK-02 | API chạy thành công, lưu DB. |
| TSK-04 | Core | Cấu hình `hypersistence-utils` JSONB | TSK-01 | Lưu Map<String, Object> thành JSONB thành công. |
| TSK-05 | Core | Viết Validation Engine với Caffeine | TSK-03 | Chặn được payload sai cấu hình. |
| TSK-06 | RuleEngine| Viết job chạy SQL động từ metadata | TSK-02 | Log ra kết quả query thành công vào ban đêm. |\n