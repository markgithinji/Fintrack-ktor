-- V18__Rename_transaction_cost_to_fees.sql

-- 1. Standardize names (Trim and normalize casing for critical categories)
UPDATE categories
SET name = 'Transaction Fees', is_default = TRUE
WHERE LOWER(TRIM(name)) IN ('transaction cost', 'transaction');

UPDATE categories
SET name = 'Other Income', is_default = TRUE
WHERE LOWER(TRIM(name)) IN ('other', 'other income', 'income') AND is_expense = FALSE;

-- 2. General cleanup: Deduplicate ALL categories per user (case-insensitive name + is_expense)
-- We keep the oldest record (presumably the default one if seeded first)
DELETE FROM categories
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY user_id, LOWER(TRIM(name)), is_expense
                   ORDER BY created_at ASC, is_default DESC
               ) as row_num
        FROM categories
    ) t
    WHERE row_num > 1
);

-- 3. Final pass to ensure system integrity for 'Transaction Fees'
-- Ensure it exists for all users who have transactions with costs but maybe missed the category
INSERT INTO categories (id, user_id, name, is_expense, icon_name, is_default, created_at)
SELECT gen_random_uuid(), users.id, 'Transaction Fees', TRUE, 'Receipt', TRUE, NOW()
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM categories
    WHERE categories.user_id = users.id
    AND categories.name = 'Transaction Fees'
    AND categories.is_expense = TRUE
)
ON CONFLICT DO NOTHING;

-- 4. Final pass for 'Other Income'
INSERT INTO categories (id, user_id, name, is_expense, icon_name, is_default, created_at)
SELECT gen_random_uuid(), users.id, 'Other Income', FALSE, 'AttachMoney', TRUE, NOW()
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM categories
    WHERE categories.user_id = users.id
    AND categories.name = 'Other Income'
    AND categories.is_expense = FALSE
)
ON CONFLICT DO NOTHING;
