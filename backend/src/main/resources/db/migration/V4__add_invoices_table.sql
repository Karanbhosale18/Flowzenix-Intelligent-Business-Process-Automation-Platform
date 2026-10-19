-- V4: Add invoices table for Invoice Intelligence module

CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT REFERENCES requests(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    vendor_name VARCHAR(255),
    invoice_number VARCHAR(100),
    invoice_date DATE,
    subtotal NUMERIC(15, 2),
    tax_amount NUMERIC(15, 2),
    total_amount NUMERIC(15, 2),
    currency VARCHAR(10) DEFAULT 'USD',
    validation_status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    validation_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_invoices_request_id ON invoices(request_id);
CREATE INDEX IF NOT EXISTS idx_invoices_created_by ON invoices(created_by);
