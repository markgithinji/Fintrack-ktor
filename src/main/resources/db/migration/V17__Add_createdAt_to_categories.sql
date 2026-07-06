-- V17__Add_createdAt_to_categories.sql
ALTER TABLE categories ADD COLUMN created_at TIMESTAMP;

-- Set a default early timestamp for existing categories
UPDATE categories SET created_at = '2024-01-01 00:00:00' WHERE created_at IS NULL;

-- Make it NOT NULL and set default for future inserts
ALTER TABLE categories ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE categories ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- Add index for sorting
CREATE INDEX idx_categories_created_at ON categories(created_at);
