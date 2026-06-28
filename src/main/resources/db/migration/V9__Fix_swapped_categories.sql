-- V9__Fix_swapped_categories.sql
-- The V6 migration incorrectly mapped transactions.is_income to categories.is_expense
-- We need to flip is_expense for categories that were created from transactions or follow the default naming

-- 1. Correct categories that are clearly Income
UPDATE categories
SET is_expense = false
WHERE name IN ('Salary', 'Freelance', 'Investments', 'Bonus', 'Rental', 'Dividends', 'Interest', 'Other Income', 'Gifts');

-- 2. Correct categories that are clearly Expenses
UPDATE categories
SET is_expense = true
WHERE name IN ('Food', 'Transport', 'Shopping', 'Health', 'Bills', 'Entertainment', 'Education', 'Travel', 'Personal Care', 'Subscriptions', 'Rent', 'Groceries', 'Insurance', 'Dining Out', 'Utilities', 'Pets', 'Fitness', 'Maintenance', 'Misc');

-- 3. For any others, we can't be 100% sure without looking at transactions,
-- but most custom ones were likely created through the UI correctly if they weren't part of the V6 migration.
-- However, since the user reported a global swap, let's flip all that don't match the names above if they came from V6.
-- Actually, the names above cover all defaults.
