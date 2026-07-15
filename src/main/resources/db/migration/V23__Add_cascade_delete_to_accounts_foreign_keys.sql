-- V22__Add_cascade_delete_to_accounts_foreign_keys.sql

-- For transactions table
ALTER TABLE transactions
DROP CONSTRAINT IF EXISTS transactions_account_id_fkey;

ALTER TABLE transactions
ADD CONSTRAINT transactions_account_id_fkey
FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;

-- For budgets table
ALTER TABLE budgets
DROP CONSTRAINT IF EXISTS budgets_account_id_fkey;

ALTER TABLE budgets
ADD CONSTRAINT budgets_account_id_fkey
FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE;
