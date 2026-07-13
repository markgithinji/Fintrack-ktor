-- V27__Change_money_columns_to_decimal.sql

-- Transactions Table
ALTER TABLE transactions ALTER COLUMN amount TYPE DECIMAL(19, 4) USING amount::DECIMAL(19, 4);
ALTER TABLE transactions ALTER COLUMN transaction_cost TYPE DECIMAL(19, 4) USING transaction_cost::DECIMAL(19, 4);
ALTER TABLE transactions ALTER COLUMN balance TYPE DECIMAL(19, 4) USING balance::DECIMAL(19, 4);

-- Accounts Table
ALTER TABLE accounts ALTER COLUMN balance TYPE DECIMAL(19, 4) USING balance::DECIMAL(19, 4);

-- Budgets Table
ALTER TABLE budgets ALTER COLUMN "limit" TYPE DECIMAL(19, 4) USING "limit"::DECIMAL(19, 4);
