-- Add linked_sources column to accounts table
ALTER TABLE accounts ADD COLUMN linked_sources TEXT DEFAULT '[]';

-- Migrate existing data based on type
UPDATE accounts SET linked_sources = '["mpesa"]' WHERE type = 'MPESA';
UPDATE accounts SET linked_sources = '["equity"]' WHERE type = 'EQUITY';
