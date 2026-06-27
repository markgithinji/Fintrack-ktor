-- V8__Add_name_to_users.sql
ALTER TABLE users ADD COLUMN name VARCHAR(100);

-- Update existing users to have a default name derived from email
UPDATE users SET name = split_part(email, '@', 1);

-- Make it non-null after population if desired, but for now let's keep it nullable or just populated.
ALTER TABLE users ALTER COLUMN name SET NOT NULL;
