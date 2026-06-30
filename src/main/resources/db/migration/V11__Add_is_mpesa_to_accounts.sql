-- V11__Add_is_mpesa_to_accounts.sql
ALTER TABLE accounts ADD COLUMN is_mpesa BOOLEAN DEFAULT FALSE;
