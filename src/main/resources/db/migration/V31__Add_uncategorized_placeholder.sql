-- V31__Add_uncategorized_placeholder.sql
-- Adds a fallback category with a nil UUID to handle transactions where the category cannot be resolved.
-- This prevents foreign key constraint violations during M-Pesa sync.

INSERT INTO categories (id, name, is_expense, icon_name, is_default, user_id, created_at)
VALUES ('00000000-0000-0000-0000-000000000000', 'Uncategorized', true, 'HelpOutline', false, NULL, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
