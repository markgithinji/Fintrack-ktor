-- V3__Add_timestamps_to_budgets.sql
ALTER TABLE budgets ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE budgets ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Create index for sorting
CREATE INDEX idx_budgets_updated_at ON budgets(updated_at);
