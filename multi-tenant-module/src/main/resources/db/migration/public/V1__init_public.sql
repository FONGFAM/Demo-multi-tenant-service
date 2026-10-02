-- Khởi tạo bảng định tuyến Tenant (Chỉ chạy trên Schema 'public')
CREATE TABLE IF NOT EXISTS tenant_registry (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) UNIQUE NOT NULL,
    schema_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);
