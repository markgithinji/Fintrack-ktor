ALTER TABLE transactions ADD COLUMN external_id VARCHAR(100);
CREATE UNIQUE INDEX transactions_external_id_user_id_unique ON transactions (external_id, user_id);
