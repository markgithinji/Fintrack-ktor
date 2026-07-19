-- Convert 'GENERAL' to 'OTHER'
UPDATE accounts SET type = 'OTHER' WHERE type = 'GENERAL';

-- Convert 'EQUITY' or 'CHECKING' to 'BANK'
UPDATE accounts SET type = 'BANK' WHERE type IN ('EQUITY', 'CHECKING');

-- Update the default value for the type column to 'OTHER'
ALTER TABLE accounts ALTER COLUMN type SET DEFAULT 'OTHER';

-- Ensure sync functionality is maintained for accounts that relied on their type
-- M-Pesa accounts should have 'mpesa' in linked_sources
UPDATE accounts
SET linked_sources = '["mpesa"]'
WHERE type = 'MPESA' AND (linked_sources IS NULL OR linked_sources = '[]' OR linked_sources = '');

-- Bank accounts with 'Equity' in their name should have 'equity' in linked_sources
UPDATE accounts
SET linked_sources = '["equity"]'
WHERE type = 'BANK' AND name ILIKE '%equity%' AND (linked_sources IS NULL OR linked_sources = '[]' OR linked_sources = '');
