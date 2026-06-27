-- V7__Add_is_default_to_categories.sql
-- Safely add the column if it doesn't already exist from a modified V6
ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure all current categories are marked as default if that's the desired state
UPDATE categories SET is_default = TRUE WHERE is_default = FALSE;
