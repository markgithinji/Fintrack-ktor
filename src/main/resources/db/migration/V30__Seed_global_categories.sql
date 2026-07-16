-- V30__Seed_global_categories.sql
-- Seeds the standardized global categories with fixed UUIDs to match the mobile app's parser.
-- This ensures that transaction imports with inferred categories don't violate foreign key constraints.

-- 1. Ensure user_id is nullable (This might have been missed if V29 was edited after it was already applied)
ALTER TABLE categories ALTER COLUMN user_id DROP NOT NULL;

-- 2. Seed the categories
INSERT INTO categories (id, name, is_expense, icon_name, is_default, user_id, created_at) VALUES
-- Expense categories (Base: 00000000-0000-4000-a000-)
('00000000-0000-4000-a000-000000000001', 'Food', true, 'Fastfood', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000002', 'Transport', true, 'DirectionsCar', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000003', 'Shopping', true, 'ShoppingCart', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000004', 'Health', true, 'LocalHospital', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000005', 'Bills', true, 'Receipt', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000006', 'Entertainment', true, 'Movie', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000007', 'Education', true, 'School', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000008', 'Gifts', true, 'CardGiftcard', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000009', 'Travel', true, 'Flight', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000010', 'Personal Care', true, 'ContentCut', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000011', 'Subscriptions', true, 'Subscriptions', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000012', 'Rent', true, 'Home', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000013', 'Groceries', true, 'ShoppingBag', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000014', 'Insurance', true, 'Shield', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000015', 'Dining Out', true, 'Restaurant', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000016', 'Utilities', true, 'Lightbulb', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000017', 'Internet', true, 'Wifi', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000018', 'Airtime', true, 'Smartphone', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000019', 'Bank', true, 'AccountBalance', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000020', 'Loans', true, 'AccountBalanceWallet', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000021', 'Charity', true, 'VolunteerActivism', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000022', 'Government', true, 'AccountBalance', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000023', 'Savings', true, 'Savings', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000024', 'Transfer', true, 'Sync', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000025', 'Pets', true, 'Pets', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000026', 'Fitness', true, 'FitnessCenter', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000027', 'Maintenance', true, 'Build', true, NULL, CURRENT_TIMESTAMP),
('00000000-0000-4000-a000-000000000028', 'Transaction Fees', true, 'Receipt', true, NULL, CURRENT_TIMESTAMP),
('99999999-9999-4999-a999-999999999999', 'Misc', true, 'HelpOutline', true, NULL, CURRENT_TIMESTAMP),

-- Income categories (Base: aaaaaaaa-aaaa-4aaa-baaa-)
('aaaaaaaa-aaaa-4aaa-baaa-000000000001', 'Salary', false, 'AttachMoney', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000002', 'Freelance', false, 'Work', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000003', 'Investments', false, 'TrendingUp', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000004', 'Gifts', false, 'CardGiftcard', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000005', 'Bonus', false, 'Paid', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000006', 'Rental', false, 'RealEstateAgent', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000007', 'Dividends', false, 'Analytics', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000008', 'Interest', false, 'Percent', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000009', 'Loans', false, 'AccountBalanceWallet', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000010', 'Transfer', false, 'Sync', true, NULL, CURRENT_TIMESTAMP),
('aaaaaaaa-aaaa-4aaa-baaa-000000000011', 'Savings', false, 'Savings', true, NULL, CURRENT_TIMESTAMP),
('bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb', 'Other Income', false, 'AttachMoney', true, NULL, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    is_expense = EXCLUDED.is_expense,
    icon_name = EXCLUDED.icon_name,
    is_default = EXCLUDED.is_default,
    user_id = EXCLUDED.user_id;
