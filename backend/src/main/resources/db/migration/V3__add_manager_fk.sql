-- V3__add_manager_fk.sql
-- Add manager self-referencing foreign key and index

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_users_manager'
          AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_manager
                FOREIGN KEY (manager_id)
                REFERENCES users(id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_manager_id
    ON users(manager_id);
