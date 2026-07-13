-- V26__Drop_legacy_budget_categories.sql

-- Drop the legacy 'categories' column that was replaced by 'category_ids'
-- This fixes the 500 error on budget creation due to NOT NULL constraint violation
ALTER TABLE budgets DROP COLUMN categories;

-- Also make account_id nullable to match the current Exposed mapping
ALTER TABLE budgets ALTER COLUMN account_id DROP NOT NULL;
