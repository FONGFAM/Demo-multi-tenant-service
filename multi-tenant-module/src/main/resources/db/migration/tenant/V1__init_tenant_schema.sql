-- Khởi tạo 12 Bảng Cốt lõi cho 1 Tenant Schema (Ví dụ: tenant_123)

-- 1. BẢNG HỆ THỐNG / METADATA
CREATE TABLE IF NOT EXISTS attribute_definitions (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    target_table VARCHAR(50) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    is_required BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS notification_rules (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    target_table VARCHAR(50) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    sql_formula TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS rule_execution_logs (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    rule_id UUID REFERENCES notification_rules(id),
    status VARCHAR(50),
    error_msg TEXT
);

-- 2. BẢNG NGHIỆP VỤ LÕI (9 Bảng chuẩn)

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    account_name VARCHAR(255) NOT NULL,
    industry VARCHAR(100),
    website VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    account_id UUID REFERENCES accounts(id),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    source VARCHAR(100),
    status VARCHAR(50) DEFAULT 'NEW'
);

CREATE TABLE IF NOT EXISTS opportunities (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    opportunity_name VARCHAR(255) NOT NULL,
    account_id UUID REFERENCES accounts(id),
    contact_id UUID REFERENCES contacts(id),
    amount DECIMAL(15,2),
    stage VARCHAR(50),
    expected_close_date DATE
);

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    unit_price DECIMAL(15,2)
);

CREATE TABLE IF NOT EXISTS quotes (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    opportunity_id UUID REFERENCES opportunities(id),
    total_amount DECIMAL(15,2)
);

CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    subject VARCHAR(255) NOT NULL,
    contact_id UUID REFERENCES contacts(id),
    account_id UUID REFERENCES accounts(id),
    priority VARCHAR(50),
    status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    activity_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    target_entity VARCHAR(50),
    target_id UUID,
    status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS notes (
    id UUID PRIMARY KEY,
    owner_id UUID, created_by UUID, updated_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
    custom_properties JSONB DEFAULT '{}'::jsonb,
    
    target_entity VARCHAR(50),
    target_id UUID,
    content TEXT
);

-- Tạo GIN Index cho JSONB để truy vấn siêu tốc trên TẤT CẢ các bảng
CREATE INDEX idx_leads_custom_prop ON leads USING GIN (custom_properties);
CREATE INDEX idx_accounts_custom_prop ON accounts USING GIN (custom_properties);
CREATE INDEX idx_contacts_custom_prop ON contacts USING GIN (custom_properties);
CREATE INDEX idx_opps_custom_prop ON opportunities USING GIN (custom_properties);
CREATE INDEX idx_tickets_custom_prop ON tickets USING GIN (custom_properties);
CREATE INDEX idx_activities_custom_prop ON activities USING GIN (custom_properties);
CREATE INDEX idx_notes_custom_prop ON notes USING GIN (custom_properties);
CREATE INDEX idx_products_custom_prop ON products USING GIN (custom_properties);
CREATE INDEX idx_quotes_custom_prop ON quotes USING GIN (custom_properties);
