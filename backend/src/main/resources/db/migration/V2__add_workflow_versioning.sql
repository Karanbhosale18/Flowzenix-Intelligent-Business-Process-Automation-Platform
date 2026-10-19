-- V2__add_workflow_versioning.sql
-- Add version column to workflow_definitions and update unique constraint to (workflow_type, version)

ALTER TABLE workflow_definitions ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1;

-- Drop legacy single-column unique constraint on workflow_type if present
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
        WHERE t.relname = 'workflow_definitions'
          AND c.contype = 'u'
          AND a.attname = 'workflow_type'
          AND array_length(c.conkey, 1) = 1
    ) LOOP
        EXECUTE 'ALTER TABLE workflow_definitions DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- Add compound unique constraint on (workflow_type, version)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_workflow_type_version'
    ) THEN
        ALTER TABLE workflow_definitions ADD CONSTRAINT uk_workflow_type_version UNIQUE (workflow_type, version);
    END IF;
END $$;
