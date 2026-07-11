-- V22__Add_account_ids_to_budgets.sql

-- 1. Add the new column
ALTER TABLE budgets ADD COLUMN account_ids TEXT;

-- 2. Migrate existing account_id to account_ids as a JSON array
UPDATE budgets SET account_ids = '["' || account_id::text || '"]';

-- 3. Make account_ids NOT NULL after migration
ALTER TABLE budgets ALTER COLUMN account_ids SET NOT NULL;

-- 4. Make account_id nullable so we can eventually drop it
ALTER TABLE budgets ALTER COLUMN account_id DROP NOT NULL;
