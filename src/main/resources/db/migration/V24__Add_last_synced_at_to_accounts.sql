-- V24__Add_last_synced_at_to_accounts.sql
ALTER TABLE accounts ADD COLUMN last_synced_at TIMESTAMP;
