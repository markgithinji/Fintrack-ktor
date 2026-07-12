-- V25__Add_category_ids_to_transactions_and_budgets.sql

-- 1. Add category_id to transactions
ALTER TABLE transactions ADD COLUMN category_id UUID REFERENCES categories(id) ON DELETE CASCADE;

-- 2. Populate category_id in transactions by matching name and user_id
UPDATE transactions t
SET category_id = c.id
FROM categories c
WHERE t.user_id = c.user_id
  AND t.category = c.name
  AND t.is_income = (NOT c.is_expense);

-- 3. Create index for the new foreign key
CREATE INDEX idx_transactions_category_id ON transactions(category_id);

-- 4. Add category_ids to budgets
ALTER TABLE budgets ADD COLUMN category_ids TEXT;

-- 5. Migrate existing budgets.categories (names) to category_ids (UUIDs)
-- This assumes budgets.categories is a JSON array like ["Food", "Transport"]
-- and attempts to map them to a JSON array of UUIDs.
-- If the mapping is too complex for SQL, we at least add the column.
-- Note: This syntax works in PostgreSQL.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'budgets' AND column_name = 'categories') THEN
        UPDATE budgets b
        SET category_ids = (
            SELECT json_agg(c.id::text)::text
            FROM categories c
            WHERE c.user_id = b.user_id
              AND c.name = ANY(
                SELECT json_array_elements_text(b.categories::json)
              )
              AND c.is_expense = b.is_expense
        );
    END IF;
END $$;

-- 6. Set defaults for any that failed to migrate to avoid NULL constraint issues
UPDATE budgets SET category_ids = '[]' WHERE category_ids IS NULL;
UPDATE transactions t SET category_id = (SELECT id FROM categories c WHERE c.user_id = t.user_id AND c.is_default = true LIMIT 1) WHERE category_id IS NULL;

-- 7. Make columns NOT NULL (to match Exposed mapping)
-- Note: we might need to be careful if no default category exists for some users
ALTER TABLE transactions ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE budgets ALTER COLUMN category_ids SET NOT NULL;
