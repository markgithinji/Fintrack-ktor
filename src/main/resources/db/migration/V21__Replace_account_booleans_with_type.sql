-- Add the new type column
ALTER TABLE accounts ADD COLUMN type VARCHAR(20) DEFAULT 'GENERAL';

-- Migrate data from old boolean columns to the new type column
UPDATE accounts SET type = 'MPESA' WHERE is_mpesa = TRUE;
UPDATE accounts SET type = 'EQUITY' WHERE is_equity = TRUE;

-- Remove the legacy boolean columns
ALTER TABLE accounts DROP COLUMN is_mpesa;
ALTER TABLE accounts DROP COLUMN is_equity;
