-- V16__Add_createdAt_to_accounts.sql
ALTER TABLE accounts ADD COLUMN created_at TIMESTAMP;

-- Update existing accounts with early timestamps for system accounts
UPDATE accounts SET created_at = '2024-01-01 00:00:00' WHERE name ILIKE '%mpesa%';
UPDATE accounts SET created_at = '2024-01-01 00:00:01' WHERE name ILIKE '%bank%';
UPDATE accounts SET created_at = '2024-01-01 00:00:02' WHERE name ILIKE '%wallet%';
UPDATE accounts SET created_at = '2024-01-01 00:00:03' WHERE name ILIKE '%savings%';
UPDATE accounts SET created_at = '2024-01-01 00:00:04' WHERE name ILIKE '%cash%';

-- Set a default for any other existing accounts
UPDATE accounts SET created_at = '2024-01-01 00:00:10' WHERE created_at IS NULL;

-- Make it NOT NULL and set default for future inserts
ALTER TABLE accounts ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE accounts ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- Add index for sorting
CREATE INDEX idx_accounts_created_at ON accounts(created_at);
