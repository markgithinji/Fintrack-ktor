-- V29__Normalize_schema_and_add_audit.sql

-- 1. Create User Tracked Categories join table
CREATE TABLE user_tracked_categories (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, category_id)
);

-- Migrate data for user_tracked_categories
-- Split CSV names and find matching UUIDs
DO $$
DECLARE
    u_rec RECORD;
    cat_name TEXT;
BEGIN
    FOR u_rec IN SELECT id, tracked_categories FROM users WHERE tracked_categories IS NOT NULL AND tracked_categories != '' LOOP
        FOR cat_name IN SELECT unnest(string_to_array(u_rec.tracked_categories, ',')) LOOP
            INSERT INTO user_tracked_categories (user_id, category_id)
            SELECT u_rec.id, id FROM categories
            WHERE user_id = u_rec.id AND name = trim(cat_name)
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- 2. Create Budget Accounts join table
CREATE TABLE budget_accounts (
    budget_id UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    PRIMARY KEY (budget_id, account_id)
);

-- Migrate data for budget_accounts (from JSON array in account_ids)
INSERT INTO budget_accounts (budget_id, account_id)
SELECT id, (json_array_elements_text(account_ids::json))::UUID
FROM budgets
WHERE account_ids IS NOT NULL AND account_ids != '' AND account_ids != '[]';

-- 3. Create Budget Categories join table
CREATE TABLE budget_categories (
    budget_id UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (budget_id, category_id)
);

-- Migrate data for budget_categories (from JSON array in category_ids)
INSERT INTO budget_categories (budget_id, category_id)
SELECT id, (json_array_elements_text(category_ids::json))::UUID
FROM budgets
WHERE category_ids IS NOT NULL AND category_ids != '' AND category_ids != '[]';

-- 4. Clean up legacy columns
ALTER TABLE users DROP COLUMN tracked_categories;
ALTER TABLE budgets DROP COLUMN account_ids;
ALTER TABLE budgets DROP COLUMN category_ids;

-- 5. Transactions Table Improvements
-- Add audit timestamps
ALTER TABLE transactions ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE transactions ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Create indexes for audit timestamps
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_updated_at ON transactions(updated_at);

-- Remove redundant category string column (since we have category_id now)
ALTER TABLE transactions DROP COLUMN category;
