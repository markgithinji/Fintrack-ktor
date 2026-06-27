-- V6__Create_categories_table.sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    is_expense BOOLEAN NOT NULL,
    icon_name VARCHAR(100),
    UNIQUE(user_id, name, is_expense)
);

CREATE INDEX idx_categories_user_id ON categories(user_id);

-- Optional: Migrate existing categories from transactions to the new table
INSERT INTO categories (user_id, name, is_expense)
SELECT DISTINCT user_id, category, is_income
FROM transactions
ON CONFLICT DO NOTHING;
