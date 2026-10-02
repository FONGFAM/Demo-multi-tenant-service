# 12 - Operations & Observability

## 1. Monitoring & Logging
- **MDC (Mapped Diagnostic Context):** Tất cả log in ra từ hệ thống phải tự động đính kèm `[tenant_id]` thông qua SLF4J MDC để dễ dàng trace lỗi trên ELK/Grafana Loki.
- **Metrics (Micrometer/Prometheus):** 
  - Đo độ trễ query JSONB (P95, P99).
  - Tỉ lệ Cache Hit/Miss của Metadata Cache.
  - Số lượng Rule được thực thi thành công/thất bại mỗi đêm.

## 2. Backup & Restore
- Do dùng chung 1 DB, việc Backup sẽ backup toàn bộ các schema cùng lúc bằng `pg_dump`.
- Khi cần restore 1 tenant bị lỗi: Tạo DB tạm -> Restore toàn bộ DB tạm -> Trích xuất riêng Schema của tenant đó (`pg_dump -n tenant_abc`) -> Đổ ngược lại vào DB chính.\n