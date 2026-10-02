ALTER TABLE tenant_registry ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
UPDATE tenant_registry SET status = 'ACTIVE';
