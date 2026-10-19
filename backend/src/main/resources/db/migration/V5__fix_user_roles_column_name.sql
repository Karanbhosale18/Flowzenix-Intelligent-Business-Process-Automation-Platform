
-- Align user_roles with the Hibernate entity mapping.
-- Preserve existing role data and the primary key.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'user_roles'
          AND column_name = 'roles'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'user_roles'
          AND column_name = 'role'
    ) THEN
ALTER TABLE user_roles
    RENAME COLUMN roles TO role;
END IF;
END $$;
